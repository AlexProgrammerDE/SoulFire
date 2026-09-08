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
package com.soulfiremc.server;

import com.google.gson.JsonPrimitive;
import com.soulfiremc.server.account.AuthType;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.account.service.OnlineSimpleJavaData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AccountRefreshTest {
  private static MinecraftAccount account() {
    return new MinecraftAccount(AuthType.MICROSOFT_JAVA_ACCESS_TOKEN, UUID.randomUUID(), "bot",
      new OnlineSimpleJavaData("old", 0), Map.of(), Map.of());
  }

  @Test
  void refreshPreservesConcurrentSettingsAndOtherAccountChanges() {
    var original = account();
    var edited = original.withSettings(Map.of("bot", Map.of("enabled", new JsonPrimitive(false))))
      .withPersistentMetadata(Map.of("proxy", Map.of("id", new JsonPrimitive(7))));
    var other = account();
    var refreshed = original.withAccountData(new OnlineSimpleJavaData("new", 100));
    var merged = InstanceManager.mergeRefreshedAccount(List.of(edited, other), original, refreshed);
    assertEquals(refreshed.accountData(), merged.getFirst().accountData());
    assertEquals(edited.settings(), merged.getFirst().settings());
    assertEquals(edited.persistentMetadata(), merged.getFirst().persistentMetadata());
    assertSame(other, merged.getLast());
  }

  @Test
  void delayedRefreshCannotRestoreRemovedAccountsOrReplaceNewCredentials() {
    var original = account();
    var refreshed = original.withAccountData(new OnlineSimpleJavaData("refreshed", 100));
    var reauthenticated = original.withAccountData(new OnlineSimpleJavaData("replacement", 200));
    assertEquals(List.of(), InstanceManager.mergeRefreshedAccount(List.of(), original, refreshed));
    assertEquals(List.of(reauthenticated),
      InstanceManager.mergeRefreshedAccount(List.of(reauthenticated), original, refreshed));
    assertThrows(IllegalArgumentException.class,
      () -> InstanceManager.mergeRefreshedAccount(List.of(original), original, account()));
  }
}
