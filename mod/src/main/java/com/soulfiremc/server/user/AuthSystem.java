/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
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
import io.jsonwebtoken.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.hibernate.SessionFactory;
import org.slf4j.event.Level;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Getter
@Slf4j
public final class AuthSystem {
  public static final UUID ROOT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  public static final String ROOT_DEFAULT_EMAIL = "root@soulfiremc.com";
  private final LogServiceImpl.StateHolder logService;
  private final ServerSettingsSource settingsSource;
  private final SecretKey jwtSecretKey;
  private final JwtParser parser;
  private final SessionFactory sessionFactory;

  public AuthSystem(SoulFireServer soulFireServer, SessionFactory sessionFactory) {
    this.logService = soulFireServer.logStateHolder();
    this.jwtSecretKey = soulFireServer.jwtSecretKey();
    this.parser = Jwts.parser().verifyWith(soulFireServer.jwtSecretKey()).build();
    this.settingsSource = soulFireServer.settingsSource();
    this.sessionFactory = sessionFactory;

    createRootUser();
  }

  private void createRootUser() {
    sessionFactory.inTransaction(s -> {
      var currentRootUser = s.find(UserEntity.class, ROOT_USER_ID);
      if (currentRootUser == null) {
        var collidingUser = s.createQuery("FROM UserEntity WHERE username = :username", UserEntity.class)
          .setParameter("username", "root")
          .uniqueResult();
        if (collidingUser != null) {
          collidingUser.username("old-root-%s".formatted(UUID.randomUUID().toString().substring(0, 6)));
          collidingUser.email("old-%s-%s".formatted(UUID.randomUUID().toString().substring(0, 6), collidingUser.email()));
          s.merge(collidingUser);
        }
      }
    });

    sessionFactory.inTransaction(s -> {
      var currentRootUser = s.find(UserEntity.class, ROOT_USER_ID);
      if (currentRootUser == null) {
        var rootUser = new UserEntity();
        rootUser.id(ROOT_USER_ID);
        rootUser.username("root");
        rootUser.role(UserEntity.Role.ADMIN);
        rootUser.email(ROOT_DEFAULT_EMAIL);
        rootUser.minIssuedAt(Instant.now());
        s.persist(rootUser);

        log.warn("The root user email is defaulted to '{}'. Please change it using the command 'set-email <email>'", ROOT_DEFAULT_EMAIL);
      } else {
        currentRootUser.role(UserEntity.Role.ADMIN);
        s.merge(currentRootUser);

        if (ROOT_DEFAULT_EMAIL.equals(currentRootUser.email())) {
          log.warn("The root user email is currently set to '{}'. Please change it using the command 'set-email <email>'", ROOT_DEFAULT_EMAIL);
        }
      }
    });
  }

  public Optional<SoulFireUser> authenticateByHeader(String authorization, String audience) {
    if (authorization.startsWith("Bearer ")) {
      return authenticateByToken(authorization.substring("Bearer ".length()), audience);
    } else if (authorization.startsWith("Basic ")) {
      var credentials = authorization.substring("Basic ".length());
      var decoded = new String(Base64.getDecoder().decode(credentials), StandardCharsets.UTF_8);
      var parts = decoded.split(":", 2);
      if (parts.length != 2) {
        return Optional.empty();
      }

      return authenticateByToken(parts[1], audience);
    } else {
      return Optional.empty();
    }
  }

  public Optional<SoulFireUser> authenticateByToken(String token, String audience) {
    Jws<Claims> claims;
    try {
      // verify token signature and parse claims
      claims = parser.parseSignedClaims(token);
    } catch (JwtException _) {
      return Optional.empty();
    }

    if (claims == null
      || claims.getPayload().getAudience() == null
      || !claims.getPayload().getAudience().contains(audience)) {
      return Optional.empty();
    }

    return authenticateBySubject(
      UUID.fromString(claims.getPayload().getSubject()),
      claims.getPayload().getIssuedAt().toInstant()
    );
  }

  public Optional<SoulFireUser> authenticateBySubject(UUID uuid, Instant issuedAt) {
    return sessionFactory.fromTransaction(s -> {
      var userEntity = s.find(UserEntity.class, uuid);
      if (userEntity == null) {
        return Optional.empty();
      }

      // Used to prevent old/stolen JWTs from being used
      // Truncate to seconds because JWTs only have second precision
      if (issuedAt.isBefore(userEntity.minIssuedAt().truncatedTo(ChronoUnit.SECONDS))) {
        return Optional.empty();
      }

      // Only update login if last login was more than 30 seconds ago
      if (userEntity.lastLoginAt() == null
        || userEntity.lastLoginAt().isBefore(Instant.now().minusSeconds(30))) {
        userEntity.lastLoginAt(Instant.now());
        s.merge(userEntity);
      }

      return Optional.of(new SoulFireUserImpl(logService, userEntity, sessionFactory, settingsSource, issuedAt));
    });
  }

  public void deleteUser(UUID uuid) {
    if (uuid.equals(ROOT_USER_ID)) {
      throw new IllegalArgumentException("Cannot delete root user");
    }

    sessionFactory.inTransaction(s -> {
      var userEntity = s.find(UserEntity.class, uuid);
      if (userEntity == null) {
        return;
      }

      s.remove(userEntity);
    });

    logService.disconnect(uuid);
  }

  public UserEntity rootUserData() {
    return getUserData(ROOT_USER_ID).orElseThrow();
  }

  public Optional<UserEntity> getUserData(UUID uuid) {
    return Optional.ofNullable(sessionFactory.fromTransaction(s -> s.find(UserEntity.class, uuid)));
  }

  public String generateJWT(UserEntity user, String audience) {
    return Jwts.builder()
      .signWith(jwtSecretKey, Jwts.SIG.HS256)
      .claims()
      .subject(user.id().toString())
      .issuedAt(Date.from(Instant.now()))
      .audience()
      .add(audience)
      .and()
      .and()
      .compact();
  }

  private record SoulFireUserImpl(
    LogServiceImpl.StateHolder logService,
    UserEntity userData,
    SessionFactory sessionFactory,
    ServerSettingsSource settingsSource,
    Instant issuedAt
  ) implements SoulFireUser {
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
    public Instant getIssuedAt() {
      return issuedAt;
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
          case GLOBAL_COMMAND_EXECUTION,
               DELETE_USER, UPDATE_USER, READ_USER,
               CREATE_USER, UPDATE_SERVER_CONFIG, READ_SERVER_CONFIG,
               GLOBAL_SUBSCRIBE_LOGS, INVALIDATE_SESSIONS,
               GENERATE_API_TOKEN -> TriState.FALSE;
          case CREATE_INSTANCE -> TriState.byBoolean(settingsSource.get(ServerSettings.ALLOW_CREATING_INSTANCES));
          case UPDATE_SELF_USERNAME -> TriState.byBoolean(settingsSource.get(ServerSettings.ALLOW_UPDATING_SELF_USERNAME));
          case UPDATE_SELF_EMAIL -> TriState.byBoolean(settingsSource.get(ServerSettings.ALLOW_UPDATING_SELF_EMAIL));
          case READ_CLIENT_DATA, GENERATE_SELF_WEBDAV_TOKEN, GENERATE_SELF_API_TOKEN,
               INVALIDATE_SELF_SESSIONS -> TriState.TRUE;
          case UNRECOGNIZED -> throw new IllegalStateException("Unexpected value: " + global.globalPermission());
        };
        case PermissionContext.InstanceContext instance -> switch (instance.instancePermission()) {
          case DELETE_INSTANCE -> TriState.byBoolean(isOwnerOfInstance(instance.instanceId())
            && settingsSource.get(ServerSettings.ALLOW_DELETING_INSTANCES));
          case UPDATE_INSTANCE_META -> TriState.byBoolean(isOwnerOfInstance(instance.instanceId())
            && settingsSource.get(ServerSettings.ALLOW_CHANGING_INSTANCE_META));
          case INSTANCE_COMMAND_EXECUTION, INSTANCE_SUBSCRIBE_LOGS,
               ACCESS_OBJECT_STORAGE,
               DOWNLOAD_URL, CHECK_PROXY, AUTHENTICATE_MC_ACCOUNT,
               CHANGE_INSTANCE_STATE, UPDATE_INSTANCE_CONFIG,
               READ_INSTANCE, READ_INSTANCE_AUDIT_LOGS,
               READ_BOT_INFO, UPDATE_BOT_CONFIG -> TriState.byBoolean(isOwnerOfInstance(instance.instanceId()));
          case UNRECOGNIZED -> throw new IllegalStateException("Unexpected value: " + instance.instancePermission());
        };
      };
    }

    @Override
    public void sendMessage(Level level, Component message) {
      logService.sendPersonalMessage(userData.id(), SoulFireAdventure.TRUE_COLOR_ANSI_SERIALIZER.serialize(message));
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
