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
package com.soulfiremc.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.soulfiremc.grpc.generated.GlobalPermission;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.settings.lib.ServerSettingsImpl;
import com.soulfiremc.server.settings.server.ServerSettings;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.PortHelper;
import com.soulfiremc.server.util.structs.GsonInstance;
import com.soulfiremc.shared.SFLogAppender;
import com.soulfiremc.test.utils.TestBootstrap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AuthSystemRootDefaultEmailTest {
  private static final String ROOT_DEFAULT_EMAIL_PROPERTY = "sf.root.default.email";

  @TempDir
  public Path tempDir;

  @Test
  void jvmFlagTakesPrecedenceOverEnvOverride() {
    assertEquals(
      "jvm-admin@example.com",
      AuthSystem.resolveRootDefaultEmail("jvm-admin@example.com", "env-admin@example.com")
    );
  }

  @Test
  void envOverrideIsUsedWhenJvmFlagIsMissing() {
    assertEquals(
      "env-admin@example.com",
      AuthSystem.resolveRootDefaultEmail(null, " env-admin@example.com ")
    );
  }

  @Test
  void invalidOverridesFallBackToBuiltInDefault() {
    assertEquals(
      AuthSystem.ROOT_DEFAULT_EMAIL,
      AuthSystem.resolveRootDefaultEmail("not-an-email", "still-not-an-email")
    );
  }

  @Test
  void explicitOverrideIsNotTreatedAsTheBuiltInDefault() {
    var previousRootDefaultEmail = System.getProperty(ROOT_DEFAULT_EMAIL_PROPERTY);

    try {
      System.setProperty(ROOT_DEFAULT_EMAIL_PROPERTY, "bootstrap-admin@example.com");

      assertFalse(AuthSystem.isDefaultRootEmail("bootstrap-admin@example.com"));
    } finally {
      restoreProperty(ROOT_DEFAULT_EMAIL_PROPERTY, previousRootDefaultEmail);
    }
  }

  @Test
  void startupInitializesRootIdentityAndLiveUserPermissionSettings() {
    var previousBaseDir = System.getProperty("sf.baseDir");
    var previousUnitTest = System.getProperty("sf.unit.test");
    var previousRootDefaultEmail = System.getProperty(ROOT_DEFAULT_EMAIL_PROPERTY);

    try {
      System.setProperty("sf.baseDir", tempDir.toAbsolutePath().toString());
      System.setProperty("sf.unit.test", "true");
      System.setProperty(ROOT_DEFAULT_EMAIL_PROPERTY, "bootstrap-admin@example.com");

      TestBootstrap.bootstrapForTest();
      SFLogAppender.INSTANCE.start();

      var server = new SoulFireServer("127.0.0.1", PortHelper.getRandomAvailablePort(), Instant.now());
      try {
        assertEquals(
          "bootstrap-admin@example.com",
          server.authSystem().rootUserData().getEmail()
        );
        var userId = UUID.randomUUID();
        server.dsl().execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
          userId.toString(), "permission-user", "permission-user@example.com", "USER");
        var user = server.authSystem().authenticateBySubject(userId, Instant.now()).orElseThrow();
        var createInstance = PermissionContext.global(GlobalPermission.CREATE_INSTANCE);
        assertTrue(user.hasPermission(createInstance));
        assertFalse(user.hasPermission(PermissionContext.global(GlobalPermission.READ_SERVER_CONFIG)));

        var setting = ServerSettings.ALLOW_CREATING_INSTANCES;
        for (var allowed : new boolean[]{false, true}) {
          var stem = new ServerSettingsImpl.Stem(Map.of(setting.namespace(),
            Map.<String, JsonElement>of(setting.key(), new JsonPrimitive(allowed))));
          server.dsl().update(Tables.SERVER_CONFIG)
            .set(Tables.SERVER_CONFIG.SETTINGS, GsonInstance.CONFIG_GSON.toJson(stem.serializeToTree()))
            .where(Tables.SERVER_CONFIG.ID.eq(1L)).execute();
          server.configUpdateHook();
          assertEquals(allowed, user.hasPermission(createInstance));
        }
      } finally {
        server.shutdownManager().shutdownSoftware(false);
        server.shutdownManager().awaitShutdown();
      }
    } finally {
      restoreProperty("sf.baseDir", previousBaseDir);
      restoreProperty("sf.unit.test", previousUnitTest);
      restoreProperty(ROOT_DEFAULT_EMAIL_PROPERTY, previousRootDefaultEmail);
    }
  }

  private static void restoreProperty(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }
}
