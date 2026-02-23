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
import com.soulfiremc.server.database.UserRole;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.database.generated.tables.records.UsersRecord;
import com.soulfiremc.server.grpc.LogServiceImpl;
import com.soulfiremc.server.settings.lib.ServerSettingsSource;
import com.soulfiremc.server.settings.server.ServerSettings;
import io.jsonwebtoken.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.event.Level;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
  private final DSLContext dsl;

  public AuthSystem(SoulFireServer soulFireServer, DSLContext dsl) {
    this.logService = soulFireServer.logStateHolder();
    this.jwtSecretKey = soulFireServer.jwtSecretKey();
    this.parser = Jwts.parser().verifyWith(soulFireServer.jwtSecretKey()).build();
    this.settingsSource = soulFireServer.settingsSource();
    this.dsl = dsl;

    createRootUser();
  }

  private void createRootUser() {
    dsl.transaction(cfg -> {
      var ctx = DSL.using(cfg);
      var currentRootUser = ctx.selectFrom(Tables.USERS).where(Tables.USERS.ID.eq(ROOT_USER_ID.toString())).fetchOne();
      if (currentRootUser == null) {
        // Use USERNAME in the WHERE clause (not ID) to handle old Hibernate databases
        // where UUIDs may be stored in a binary format that doesn't match text comparison
        var randomSuffix = UUID.randomUUID().toString().substring(0, 6);
        ctx.update(Tables.USERS)
          .set(Tables.USERS.USERNAME, "old-root-%s".formatted(randomSuffix))
          .set(Tables.USERS.EMAIL, "old-root-%s@soulfiremc.com".formatted(randomSuffix))
          .set(Tables.USERS.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.USERS.USERNAME.eq("root"))
          .execute();
      }
    });

    dsl.transaction(cfg -> {
      var ctx = DSL.using(cfg);
      var currentRootUser = ctx.selectFrom(Tables.USERS).where(Tables.USERS.ID.eq(ROOT_USER_ID.toString())).fetchOne();
      if (currentRootUser == null) {
        var now = LocalDateTime.now(ZoneOffset.UTC);
        ctx.insertInto(Tables.USERS)
          .set(Tables.USERS.ID, ROOT_USER_ID.toString())
          .set(Tables.USERS.USERNAME, "root")
          .set(Tables.USERS.ROLE, UserRole.ADMIN.name())
          .set(Tables.USERS.EMAIL, ROOT_DEFAULT_EMAIL)
          .set(Tables.USERS.MIN_ISSUED_AT, now)
          .set(Tables.USERS.CREATED_AT, now)
          .set(Tables.USERS.UPDATED_AT, now)
          .set(Tables.USERS.VERSION, 0L)
          .execute();
        log.warn("The root user email is defaulted to '{}'. Please change it using the command 'set-email <email>'", ROOT_DEFAULT_EMAIL);
      } else {
        ctx.update(Tables.USERS)
          .set(Tables.USERS.ROLE, UserRole.ADMIN.name())
          .set(Tables.USERS.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.USERS.ID.eq(ROOT_USER_ID.toString()))
          .execute();
        if (ROOT_DEFAULT_EMAIL.equals(currentRootUser.getEmail())) {
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
    return dsl.transactionResult(cfg -> {
      var ctx = DSL.using(cfg);
      var userRecord = ctx.selectFrom(Tables.USERS).where(Tables.USERS.ID.eq(uuid.toString())).fetchOne();
      if (userRecord == null) {
        return Optional.empty();
      }

      // Used to prevent old/stolen JWTs from being used
      // Truncate to seconds because JWTs only have second precision
      var minIssuedAt = userRecord.getMinIssuedAt().toInstant(ZoneOffset.UTC);
      if (issuedAt.isBefore(minIssuedAt.truncatedTo(ChronoUnit.SECONDS))) {
        return Optional.empty();
      }

      // Only update login if last login was more than 30 seconds ago
      var lastLogin = userRecord.getLastLoginAt();
      var lastLoginInstant = lastLogin != null ? lastLogin.toInstant(ZoneOffset.UTC) : null;
      if (lastLoginInstant == null || lastLoginInstant.isBefore(Instant.now().minusSeconds(30))) {
        ctx.update(Tables.USERS)
          .set(Tables.USERS.LAST_LOGIN_AT, LocalDateTime.now(ZoneOffset.UTC))
          .set(Tables.USERS.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.USERS.ID.eq(uuid.toString()))
          .execute();
      }

      return Optional.of(new SoulFireUserImpl(logService, userRecord, dsl, settingsSource, issuedAt));
    });
  }

  public void deleteUser(UUID uuid) {
    if (uuid.equals(ROOT_USER_ID)) {
      throw new IllegalArgumentException("Cannot delete root user");
    }

    dsl.deleteFrom(Tables.USERS).where(Tables.USERS.ID.eq(uuid.toString())).execute();

    logService.disconnect(uuid);
  }

  public UsersRecord rootUserData() {
    return getUserData(ROOT_USER_ID).orElseThrow();
  }

  public Optional<UsersRecord> getUserData(UUID uuid) {
    return Optional.ofNullable(dsl.selectFrom(Tables.USERS).where(Tables.USERS.ID.eq(uuid.toString())).fetchOne());
  }

  public String generateJWT(UsersRecord user, String audience) {
    return Jwts.builder()
      .signWith(jwtSecretKey, Jwts.SIG.HS256)
      .claims()
      .subject(UUID.fromString(user.getId()).toString())
      .issuedAt(Date.from(Instant.now()))
      .audience()
      .add(audience)
      .and()
      .and()
      .compact();
  }

  private record SoulFireUserImpl(
    LogServiceImpl.StateHolder logService,
    UsersRecord userData,
    DSLContext dsl,
    ServerSettingsSource settingsSource,
    Instant issuedAt
  ) implements SoulFireUser {
    @Override
    public UUID getUniqueId() {
      return UUID.fromString(userData.getId());
    }

    @Override
    public String getUsername() {
      return userData.getUsername();
    }

    @Override
    public String getEmail() {
      return userData.getEmail();
    }

    @Override
    public UserRole getRole() {
      return UserRole.valueOf(userData.getRole());
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
      logService.sendPersonalMessage(UUID.fromString(userData.getId()), SoulFireAdventure.TRUE_COLOR_ANSI_SERIALIZER.serialize(message));
    }

    private boolean isOwnerOfInstance(UUID instanceId) {
      var count = dsl.selectCount()
        .from(Tables.INSTANCES)
        .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
        .and(Tables.INSTANCES.OWNER_ID.eq(userData.getId()))
        .fetchOne(0, int.class);
      return count != null && count > 0;
    }

    private boolean isAdmin() {
      return UserRole.valueOf(userData.getRole()) == UserRole.ADMIN;
    }
  }
}
