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
package com.soulfiremc.server.plugins;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.soulfiremc.server.AttackManager;
import com.soulfiremc.server.api.PluginHelper;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.EventUtil;
import com.soulfiremc.server.api.event.attack.AttackEndedEvent;
import com.soulfiremc.server.api.event.attack.AttackInitEvent;
import com.soulfiremc.server.api.event.lifecycle.SettingsRegistryInitEvent;
import com.soulfiremc.server.protocol.SFProtocolHelper;
import com.soulfiremc.server.protocol.netty.SFTcpPacketCodec;
import com.soulfiremc.server.settings.lib.SettingsHolder;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.settings.property.StringProperty;
import com.soulfiremc.server.user.Permission;
import com.soulfiremc.server.user.ServerCommandSource;
import com.soulfiremc.util.PortHelper;
import com.soulfiremc.util.ResourceHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.util.TriState;
import net.lenni0451.lambdaevents.EventHandler;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;
import net.raphimc.netminecraft.netty.connection.NetServer;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.tcp.TcpServer;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundPingPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;

@Slf4j
public class POVServer implements InternalPlugin {
  private static final List<Class<?>> NOT_SYNCED =
    List.of(
      ClientboundKeepAlivePacket.class,
      ServerboundKeepAlivePacket.class,
      ClientboundPingPacket.class,
      ServerboundPongPacket.class,
      ClientboundCustomPayloadPacket.class,
      ServerboundFinishConfigurationPacket.class,
      ServerboundConfigurationAcknowledgedPacket.class);
  private static final byte[] FULL_LIGHT = new byte[2048];

  static {
    Arrays.fill(FULL_LIGHT, (byte) 0xFF);
  }

  @EventHandler
  public static void onSettingsRegistryInit(SettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(POVServerSettings.class, "POV Server");
  }

  @Override
  public void onLoad() {
    SoulFireAPI.registerListeners(POVServer.class);
    SoulFireAPI.registerListener(
      AttackInitEvent.class,
      event -> {
        var attackManager = event.attackManager();
        var settingsHolder = attackManager.settingsHolder();
        if (!settingsHolder.get(POVServerSettings.ENABLED)) {
          return;
        }

        var freePort =
          PortHelper.getAvailablePort(settingsHolder.get(POVServerSettings.PORT_START));
        startPOVServer(settingsHolder, freePort, attackManager);
        log.info("Started POV server on 0.0.0.0:{} for attack {}", freePort, attackManager.id());

        EventUtil.runAndAssertChanged(attackManager.eventBus(), () -> PluginHelper.registerSafeEventConsumer(
          attackManager.eventBus(),
          AttackEndedEvent.class,
          e -> {
            log.info("Stopping POV server for attack {}", attackManager.id());
          }));
      });
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class POVServerSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("pov-server");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable POV server",
        new String[] {"--pov-server"},
        "Host a POV server for the bots",
        false);
    public static final IntProperty PORT_START =
      BUILDER.ofInt(
        "port-start",
        "Port Start",
        new String[] {"--port-start"},
        "What port to start with to host the POV server",
        31765,
        1,
        65535,
        1);
    public static final BooleanProperty ENABLE_COMMANDS =
      BUILDER.ofBoolean(
        "enable-commands",
        "Enable commands",
        new String[] {"--pov-enable-commands"},
        "Allow users connected to the POV server to execute commands in the SF server shell",
        true);
    public static final StringProperty COMMAND_PREFIX =
      BUILDER.ofString(
        "command-prefix",
        "Command Prefix",
        new String[] {"--pov-command-prefix"},
        "The prefix to use for commands executed in the SF server shell",
        "#");
  }

  private static GameProfile getFakePlayerListEntry(Component text) {
    return new GameProfile(UUID.randomUUID(), LegacyComponentSerializer.legacySection().serialize(text));
  }

  private static TcpServer startPOVServer(SettingsHolder settingsHolder, int port, AttackManager attackManager) {
    var faviconBytes = ResourceHelper.getResourceAsBytes("assets/pov_favicon.png");

    var server = new NetServer(new Supplier<>() {
      @Override
      public ChannelHandler get() {
        return new MinecraftChannelInitializer(new Supplier<>() {
          @Override
          public ChannelHandler get() {
            return new SimpleChannelInboundHandler<MinecraftPacket>() {
              @Override
              public void channelActive(ChannelHandlerContext ctx) throws Exception {
                super.channelActive(ctx);
                System.out.println("New connection from " + ctx.channel().remoteAddress());
              }

              @Override
              public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                super.channelInactive(ctx);
                System.out.println(ctx.channel().remoteAddress() + " closed connection");
              }

              @Override
              protected void channelRead0(ChannelHandlerContext channelHandlerContext, MinecraftPacket packet) throws Exception {
                System.out.println("Received packet " + packet.getClass().getSimpleName());
              }
            };
          }
        }) {
          @Override
          protected void initChannel(Channel channel) {
            super.initChannel(channel);
            channel.pipeline().replace(
              MCPipeline.PACKET_CODEC_HANDLER_NAME,
              MCPipeline.PACKET_CODEC_HANDLER_NAME,
              new SFTcpPacketCodec()
            );

            channel.attr(SFProtocolHelper.SF_PACKET_REGISTRY_ATTRIBUTE_KEY).set(
              SFProtocolHelper.getRegistryForState(ProtocolState.HANDSHAKE, false));
          }
        };
      }
    });

    server.bind(new InetSocketAddress("0.0.0.0", 25565), true);

    return null;
  }

  private record PovServerUser(Session session, String username) implements ServerCommandSource {
    @Override
    public UUID getUniqueId() {
      return UUID.nameUUIDFromBytes("POVUser:%s".formatted(username).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getUsername() {
      return username;
    }

    @Override
    public TriState getPermission(Permission permission) {
      return TriState.TRUE;
    }

    @Override
    public void sendMessage(Component message) {
      session.send(new ClientboundSystemChatPacket(message, false));
    }
  }
}
