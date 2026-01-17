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
package com.soulfiremc.server.account;

import com.google.gson.JsonObject;
import com.soulfiremc.server.account.service.BedrockData;
import com.soulfiremc.server.account.service.OnlineChainJavaData;
import com.soulfiremc.server.settings.lib.BotSettingsImpl;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.util.MinecraftAuth4To5Migrator;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

public final class AuthHelpers {
  private AuthHelpers() {
  }

  public static MinecraftAccount fromBedrockAuthManager(AuthType authType, BedrockAuthManager authManager, BotSettingsImpl.@Nullable Stem settingsStem) throws IOException {
    var mcChain = authManager.getMinecraftCertificateChain().getUpToDate();
    authManager.getMsaToken().refreshIfExpired();
    authManager.getXblDeviceToken().refreshIfExpired();
    authManager.getXblUserToken().refreshIfExpired();
    authManager.getXblTitleToken().refreshIfExpired();
    authManager.getBedrockXstsToken().refreshIfExpired();
    authManager.getPlayFabXstsToken().refreshIfExpired();
    authManager.getRealmsXstsToken().refreshIfExpired();
    authManager.getXboxLiveXstsToken().refreshIfExpired();
    authManager.getPlayFabToken().refreshIfExpired();
    authManager.getMinecraftSession().refreshIfExpired();
    authManager.getMinecraftMultiplayerToken().refreshIfExpired();
    authManager.getMinecraftCertificateChain().refreshIfExpired();
    return new MinecraftAccount(
      authType,
      mcChain.getIdentityUuid(),
      mcChain.getIdentityDisplayName(),
      new BedrockData(
        BedrockAuthManager.toJson(authManager)),
      settingsStem);
  }

  public static MinecraftAccount fromJavaAuthManager(AuthType authType, JavaAuthManager authManager, BotSettingsImpl.@Nullable Stem settingsStem) throws IOException {
    var mcProfile = authManager.getMinecraftProfile().getUpToDate();
    authManager.getMinecraftToken().refreshIfExpired();
    authManager.getMinecraftPlayerCertificates().refreshIfExpired();
    return new MinecraftAccount(
      authType,
      mcProfile.getId(),
      mcProfile.getName(),
      new OnlineChainJavaData(
        JavaAuthManager.toJson(authManager)),
      settingsStem);
  }

  public static JsonObject migrateBedrockAuthChain(JsonObject oldAuthChain) {
    if (oldAuthChain.has("_saveVersion")) {
      // Already migrated
      return oldAuthChain;
    }

    return MinecraftAuth4To5Migrator.migrateBedrockSave(oldAuthChain);
  }

  public static JsonObject migrateJavaAuthChain(JsonObject oldAuthChain) {
    if (oldAuthChain.has("_saveVersion")) {
      // Already migrated
      return oldAuthChain;
    }

    return MinecraftAuth4To5Migrator.migrateJavaSave(oldAuthChain);
  }
}
