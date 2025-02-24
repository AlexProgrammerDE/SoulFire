/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.user;

import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.adventure.SoulFireAdventure;
import com.soulfiremc.server.database.InstanceEntity;
import com.soulfiremc.server.database.UserEntity;
import com.soulfiremc.server.grpc.LogServiceImpl;
import com.soulfiremc.server.settings.lib.ServerSettingsSource;
import com.soulfiremc.server.settings.server.ServerSettings;
import com.soulfiremc.server.util.KeyHelper;
import com.soulfiremc.server.util.SFPathConstants;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.hibernate.SessionFactory;
import org.slf4j.event.Level;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Getter
@Slf4j
public class AuthSystem {
  public static final String ROOT_DEFAULT_EMAIL = "root@soulfiremc.com";
  private static final String ROOT_USERNAME = "root";
  private final LogServiceImpl logService;
  private final SessionFactory sessionFactory;
  private final ServerSettingsSource settingsSource;
  private final SecretKey jwtSecretKey;
  private final UUID rootUserId;

  public AuthSystem(SoulFireServer soulFireServer) {
    this.logService = soulFireServer.injector().getSingleton(LogServiceImpl.class);
    this.jwtSecretKey = KeyHelper.getOrCreateJWTSecretKey(SFPathConstants.getSecretKeyFile(soulFireServer.baseDirectory()));
    this.sessionFactory = soulFireServer.sessionFactory();
    this.settingsSource = soulFireServer.settingsSource();
    this.rootUserId = createRootUser();
  }

  private UUID createRootUser() {
    return sessionFactory.fromTransaction((s) -> {
      var currentRootUser = s.createQuery("FROM UserEntity WHERE username = :username", UserEntity.class)
        .setParameter("username", ROOT_USERNAME)
        .uniqueResult();
      if (currentRootUser == null) {
        var rootUser = new UserEntity();
        rootUser.username("root");
        rootUser.role(UserEntity.Role.ADMIN);
        rootUser.email(ROOT_DEFAULT_EMAIL);
        s.persist(rootUser);

        log.warn("The root user email is defaulted to '{}'. Please change it using the command 'set-email <email>'", ROOT_DEFAULT_EMAIL);

        return rootUser.id();
      } else {
        currentRootUser.role(UserEntity.Role.ADMIN);
        s.merge(currentRootUser);

        if (ROOT_DEFAULT_EMAIL.equals(currentRootUser.email())) {
          log.warn("The root user email is currently set to '{}'. Please change it using the command 'set-email <email>'", ROOT_DEFAULT_EMAIL);
        }

        return currentRootUser.id();
      }
    });
  }

  public Optional<SoulFireUser> authenticate(String subject, Instant issuedAt) {
    var uuid = UUID.fromString(subject);
    return sessionFactory.fromTransaction(s -> {
      var userEntity = s.find(UserEntity.class, uuid);
      if (userEntity == null) {
        return Optional.empty();
      }

      // Used to prevent old/stolen JWTs from being used
      if (issuedAt.isBefore(userEntity.minIssuedAt())) {
        return Optional.empty();
      }

      // Only update login if last login was more than 30 seconds ago
      if (userEntity.lastLoginAt() == null
        || userEntity.lastLoginAt().isBefore(Instant.now().minusSeconds(30))) {
        userEntity.lastLoginAt(Instant.now());
        s.merge(userEntity);
      }

      return Optional.of(new SoulFireUserImpl(logService, userEntity, sessionFactory, settingsSource));
    });
  }

  public UserEntity rootUserData() {
    return getUserData(rootUserId).orElseThrow();
  }

  public Optional<UserEntity> getUserData(UUID uuid) {
    return Optional.ofNullable(sessionFactory.fromTransaction(s -> s.find(UserEntity.class, uuid)));
  }

  public String generateJWT(UserEntity user) {
    return Jwts.builder()
      .subject(user.id().toString())
      .issuedAt(Date.from(Instant.now()))
      .signWith(jwtSecretKey, Jwts.SIG.HS256)
      .compact();
  }

  private record SoulFireUserImpl(LogServiceImpl logService, UserEntity userData, SessionFactory sessionFactory, ServerSettingsSource settingsSource) implements SoulFireUser {
    @Override
    public UUID getUniqueId() {
      return userData.id();
    }

    @Override
    public String getUsername() {
      return userData.username();
    }

    @Override
    public String getEmail() {
      return userData.email();
    }

    @Override
    public UserEntity.Role getRole() {
      return userData.role();
    }

    @Override
    public TriState getPermission(PermissionContext permission) {
      // Admins have all permissions
      if (isAdmin()) {
        return TriState.TRUE;
      }

      // Permissions for normal users
      return switch (permission) {
        case PermissionContext.GlobalContext global -> switch (global.globalPermission()) {
          case GLOBAL_COMMAND_COMPLETION, GLOBAL_COMMAND_EXECUTION,
               DELETE_USER, UPDATE_USER, READ_USER,
               CREATE_USER, UPDATE_SERVER_CONFIG, READ_SERVER_CONFIG,
               GLOBAL_SUBSCRIBE_LOGS, INVALIDATE_SESSIONS -> TriState.FALSE;
          case CREATE_INSTANCE -> TriState.byBoolean(settingsSource.get(ServerSettings.ALLOW_CREATING_INSTANCES));
          case READ_CLIENT_DATA -> TriState.TRUE;
          case UNRECOGNIZED -> throw new IllegalStateException("Unexpected value: " + global.globalPermission());
        };
        case PermissionContext.InstanceContext instance -> switch (instance.instancePermission()) {
          case DELETE_INSTANCE -> TriState.byBoolean(isOwnerOfInstance(instance.instanceId())
            && settingsSource.get(ServerSettings.ALLOW_DELETING_INSTANCES));
          case UPDATE_INSTANCE_META -> TriState.byBoolean(isOwnerOfInstance(instance.instanceId())
            && settingsSource.get(ServerSettings.ALLOW_CHANGING_INSTANCE_META));
          case INSTANCE_COMMAND_EXECUTION, INSTANCE_SUBSCRIBE_LOGS, LIST_OBJECT_STORAGE,
               DELETE_OBJECT_STORAGE, DOWNLOAD_OBJECT_STORAGE, UPLOAD_OBJECT_STORAGE,
               DOWNLOAD_URL, CHECK_PROXY, AUTHENTICATE_MC_ACCOUNT,
               CHANGE_INSTANCE_STATE, UPDATE_INSTANCE_CONFIG,
               READ_INSTANCE, INSTANCE_COMMAND_COMPLETION -> TriState.byBoolean(isOwnerOfInstance(instance.instanceId()));
          case UNRECOGNIZED -> throw new IllegalStateException("Unexpected value: " + instance.instancePermission());
        };
      };
    }

    @Override
    public void sendMessage(Level level, Component message) {
      logService.sendMessage(userData.id(), SoulFireAdventure.TRUE_COLOR_ANSI_SERIALIZER.serialize(message));
    }

    private boolean isOwnerOfInstance(UUID instanceId) {
      return sessionFactory.fromTransaction(s -> {
        var instanceEntity = s.createQuery("FROM InstanceEntity WHERE id = :id AND owner = :owner", InstanceEntity.class)
          .setParameter("id", instanceId)
          .setParameter("owner", userData)
          .uniqueResult();
        return instanceEntity != null;
      });
    }

    private boolean isAdmin() {
      return userData.role() == UserEntity.Role.ADMIN;
    }
  }
}
