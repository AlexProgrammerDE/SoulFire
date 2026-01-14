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
package com.soulfiremc.server.bot;

import com.google.common.net.HostAndPort;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.BotConnectionInitEvent;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.lib.InstanceSettingsSource;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.EventLoopGroup;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.checkerframework.checker.nullness.qual.Nullable;

public record BotConnectionFactory(
  InstanceManager instanceManager,
  InstanceSettingsSource settingsSource,
  @Nullable BotEntity botEntity,
  MinecraftAccount minecraftAccount,
  ProtocolVersion protocolVersion,
  ServerAddress serverAddress,
  @Nullable
  SFProxy proxyData,
  EventLoopGroup eventLoopGroup) {
  private static final int JAVA_DEFAULT_PORT = 25565;
  private static final int BEDROCK_DEFAULT_PORT = 19132;

  public static ServerAddress parseAddress(String address, ProtocolVersion protocolVersion) {
    HostAndPort hostAndPort = HostAndPort.fromString(address)
      .withDefaultPort(BedrockProtocolVersion.bedrockLatest.equals(protocolVersion) ? BEDROCK_DEFAULT_PORT : JAVA_DEFAULT_PORT);
    if (hostAndPort.getHost().isEmpty()) {
      throw new IllegalArgumentException("Invalid host address: " + address);
    }

    return new ServerAddress(hostAndPort);
  }

  public BotConnection prepareConnection(boolean isStatusPing) {
    var botConnection =
      new BotConnection(
        this,
        instanceManager,
        settingsSource,
        botEntity,
        minecraftAccount,
        protocolVersion,
        serverAddress,
        proxyData,
        eventLoopGroup,
        isStatusPing);

    SoulFireAPI.postEvent(new BotConnectionInitEvent(botConnection));

    return botConnection;
  }
}
