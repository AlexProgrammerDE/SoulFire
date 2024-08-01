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

import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.api.event.attack.BotConnectionInitEvent;
import com.soulfiremc.server.protocol.netty.ResolveUtil;
import com.soulfiremc.server.settings.BotSettings;
import com.soulfiremc.server.settings.lib.SettingsHolder;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.proxy.SFProxy;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.EventLoopGroup;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.slf4j.Logger;

public record BotConnectionFactory(
  InstanceManager instanceManager,
  ResolveUtil.ResolvedAddress resolvedAddress,
  SettingsHolder settingsHolder,
  Logger logger,
  MinecraftAccount minecraftAccount,
  ProtocolVersion protocolVersion,
  SFProxy proxyData,
  EventLoopGroup eventLoopGroup) {
  public BotConnection prepareConnection() {
    return prepareConnectionInternal(ProtocolState.LOGIN);
  }

  public BotConnection prepareConnectionInternal(ProtocolState targetState) {
    var protocol = new MinecraftProtocol();

    // Make sure this options is set to false, we have our own listeners
    protocol.setUseDefaultListeners(false);

    var botConnection =
      new BotConnection(
        this,
        instanceManager,
        settingsHolder,
        logger,
        protocol,
        resolvedAddress,
        minecraftAccount,
        targetState,
        protocolVersion,
        proxyData,
        eventLoopGroup);

    var session = botConnection.session();
    session.setConnectTimeout(settingsHolder.get(BotSettings.CONNECT_TIMEOUT));
    session.setReadTimeout(settingsHolder.get(BotSettings.READ_TIMEOUT));
    session.setWriteTimeout(settingsHolder.get(BotSettings.WRITE_TIMEOUT));

    session.addListener(new SFBaseListener(botConnection, targetState));
    session.addListener(new SFSessionListener(botConnection));

    instanceManager.eventBus().call(new BotConnectionInitEvent(botConnection));

    return botConnection;
  }
}
