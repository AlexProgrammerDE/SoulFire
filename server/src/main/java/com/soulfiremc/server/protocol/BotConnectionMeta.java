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
package com.soulfiremc.server.protocol;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.soulfiremc.server.protocol.bot.BotControlAPI;
import com.soulfiremc.server.protocol.bot.SessionDataManager;
import com.soulfiremc.server.protocol.netty.ViaClientSession;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.account.service.OnlineJavaData;
import com.soulfiremc.settings.proxy.SFProxy;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
public class BotConnectionMeta {
  private final MinecraftAccount minecraftAccount;
  private final UUID accountProfileId;
  private final String accountName;
  private final ProtocolState targetState;
  private final ProtocolVersion protocolVersion;
  private final SFSessionService sessionService;
  @Setter private SessionDataManager sessionDataManager;
  @Setter private BotControlAPI botControlAPI;

  public BotConnectionMeta(
      MinecraftAccount minecraftAccount,
      ProtocolState targetState,
      ProtocolVersion protocolVersion,
      SFProxy proxyData) {
    this.minecraftAccount = minecraftAccount;
    this.accountProfileId = minecraftAccount.profileId();
    this.accountName = minecraftAccount.lastKnownName();
    this.targetState = targetState;
    this.protocolVersion = protocolVersion;
    this.sessionService =
        minecraftAccount.isPremiumJava()
            ? new SFSessionService(minecraftAccount.authType(), proxyData)
            : null;
  }

  public void joinServerId(String serverId, ViaClientSession session) {
    try {
      var javaData = (OnlineJavaData) minecraftAccount.accountData();
      sessionService.joinServer(accountProfileId, javaData.authToken(), serverId);
      session.logger().debug("Successfully sent mojang join request!");
    } catch (Exception e) {
      session.disconnect("Login failed: Authentication error: " + e.getMessage(), e);
    }
  }
}
