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
import com.soulfiremc.server.grpc.LogServiceImpl;
import com.soulfiremc.server.plugins.ChatMessageLogger;
import com.soulfiremc.server.util.KeyHelper;
import com.soulfiremc.server.util.SFPathConstants;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import lombok.With;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.slf4j.event.Level;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;

@Getter
public class AuthSystem {
  private static final UUID ROOT_USER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  private final SecretKey jwtSecretKey;
  private final Map<UUID, UserData> userDataMap = new HashMap<>();

  public AuthSystem(SoulFireServer soulFireServer) {
    this.jwtSecretKey = KeyHelper.getOrCreateJWTSecretKey(SFPathConstants.getSecretKeyFile(soulFireServer.baseDirectory()));
    setUserData(new UserData(
      ROOT_USER_UUID,
      "root",
      Role.ADMIN,
      Instant.EPOCH,
      Instant.EPOCH,
      Instant.EPOCH
    ));
  }

  public Optional<SoulFireUser> authenticate(String subject, Instant issuedAt) {
    var uuid = UUID.fromString(subject);
    var userData = userDataMap.get(uuid);
    if (userData == null) {
      return Optional.empty();
    }

    // Used to prevent old/stolen JWTs from being used
    if (issuedAt.isBefore(userData.minIssuedAt())) {
      return Optional.empty();
    }

    setUserData(userData.withLastLoginAt(Instant.now()));

    return Optional.of(new SoulFireUserImpl(userData));
  }

  public UserData rootUserData() {
    return getUserData(ROOT_USER_UUID).orElseThrow();
  }

  public Optional<UserData> getUserData(UUID uuid) {
    return Optional.ofNullable(userDataMap.get(uuid));
  }

  public void setUserData(UserData userData) {
    userDataMap.put(userData.id(), userData);
  }

  public String generateJWT(UserData user) {
    return Jwts.builder()
      .subject(user.id().toString())
      .issuedAt(Date.from(Instant.now()))
      .signWith(jwtSecretKey, Jwts.SIG.HS256)
      .compact();
  }

  public enum Role {
    ADMIN,
    USER
  }

  private record SoulFireUserImpl(UserData userData) implements SoulFireUser {
    @Override
    public UUID getUniqueId() {
      return userData.id();
    }

    @Override
    public String getUsername() {
      return userData.name();
    }

    @Override
    public TriState getPermission(PermissionContext permission) {
      // Admins have all permissions
      if (userData.role() == Role.ADMIN) {
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

  @With
  public record UserData(
    UUID id,
    String name,
    Role role,
    Instant createdAt,
    Instant lastLoginAt,
    Instant minIssuedAt
  ) {
  }
}
