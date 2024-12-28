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
import com.soulfiremc.server.database.UserEntity;
import com.soulfiremc.server.grpc.LogServiceImpl;
import com.soulfiremc.server.plugins.ChatMessageLogger;
import com.soulfiremc.server.util.KeyHelper;
import com.soulfiremc.server.util.SFPathConstants;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
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
public class AuthSystem {
  private static final UUID ROOT_USER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  private final SessionFactory sessionFactory;
  private final SecretKey jwtSecretKey;

  public AuthSystem(SoulFireServer soulFireServer) {
    this.jwtSecretKey = KeyHelper.getOrCreateJWTSecretKey(SFPathConstants.getSecretKeyFile(soulFireServer.baseDirectory()));
    this.sessionFactory = soulFireServer.sessionFactory();

    createRootUser();
  }

  private void createRootUser() {
    sessionFactory.inSession((s) -> {
      var currentRootUser = s.find(UserEntity.class, ROOT_USER_UUID);
      if (currentRootUser == null) {
        var rootUser = new UserEntity();
        rootUser.id(ROOT_USER_UUID);
        rootUser.username("root");
        rootUser.role(UserEntity.Role.ADMIN);
        s.merge(rootUser);
      }
    });
  }

  public Optional<SoulFireUser> authenticate(String subject, Instant issuedAt) {
    var uuid = UUID.fromString(subject);
    var optionalUser = getUserData(uuid);
    if (optionalUser.isEmpty()) {
      return Optional.empty();
    }

    var userEntity = optionalUser.get();

    // Used to prevent old/stolen JWTs from being used
    if (issuedAt.isBefore(userEntity.minIssuedAt())) {
      return Optional.empty();
    }

    userEntity.lastLoginAt(Instant.now());
    sessionFactory.inSession((s) -> s.merge(userEntity));

    return Optional.of(new SoulFireUserImpl(userEntity));
  }

  public UserEntity rootUserData() {
    return getUserData(ROOT_USER_UUID).orElseThrow();
  }

  public Optional<UserEntity> getUserData(UUID uuid) {
    return Optional.ofNullable(sessionFactory.fromSession(s -> s.find(UserEntity.class, uuid)));
  }

  public String generateJWT(UserEntity user) {
    return Jwts.builder()
      .subject(user.id().toString())
      .issuedAt(Date.from(Instant.now()))
      .signWith(jwtSecretKey, Jwts.SIG.HS256)
      .compact();
  }

  private record SoulFireUserImpl(UserEntity userData) implements SoulFireUser {
    @Override
    public UUID getUniqueId() {
      return userData.id();
    }

    @Override
    public String getUsername() {
      return userData.username();
    }

    @Override
    public TriState getPermission(PermissionContext permission) {
      // Admins have all permissions
      if (userData.role() == UserEntity.Role.ADMIN) {
        return TriState.TRUE;
      }

      // TODO: Implement permission system
      return TriState.FALSE;
    }

    @Override
    public void sendMessage(Level level, Component message) {
      LogServiceImpl.sendMessage(userData.id(), ChatMessageLogger.ANSI_MESSAGE_SERIALIZER.serialize(message));
    }
  }
}
