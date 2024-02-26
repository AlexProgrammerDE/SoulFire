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
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerSpawnInfo;
import com.github.steveice10.mc.protocol.data.game.level.notify.GameEvent;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.ServerClosedEvent;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectingEvent;
import com.github.steveice10.packetlib.event.session.PacketErrorEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpServer;
import com.soulfiremc.server.AttackManager;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.EventUtil;
import com.soulfiremc.server.api.event.attack.AttackInitEvent;
import com.soulfiremc.server.api.event.attack.BotConnectionInitEvent;
import com.soulfiremc.server.api.event.lifecycle.SettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.lib.property.BooleanProperty;
import com.soulfiremc.server.settings.lib.property.IntProperty;
import com.soulfiremc.server.settings.lib.property.Property;
import com.soulfiremc.util.PortHelper;
import java.util.ArrayList;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.lenni0451.lambdaevents.EventHandler;
import org.cloudburstmc.math.vector.Vector3i;

@Slf4j
public class POVServer implements InternalPlugin {
  @EventHandler
  public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
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

          EventUtil.runAndAssertChanged(
              attackManager.eventBus(),
              () -> {
                var freePort =
                    PortHelper.getAvailablePort(settingsHolder.get(POVServerSettings.PORT_START));
                var server = new POVServerInstance(freePort, attackManager);
                log.info(
                    "Started POV server on 0.0.0.0:{} for attack {}", freePort, attackManager.id());

                attackManager
                    .eventBus()
                    .registerConsumer(
                        (Consumer<BotConnectionInitEvent>)
                            event2 -> {
                              var botConnection = event2.connection();
                              /*
                              EventUtil.runAndAssertChanged(
                                  botConnection.eventBus(),
                                  () -> {
                                    botConnection.eventBus().registerConsumer((Consumer<BotConnectionInitEvent>) event2 -> {
                                      var botConnection = event2.connection();
                                      EventUtil.runAndAssertChanged(
                                          botConnection.eventBus(),
                                          () -> {

                                          });
                                    }, BotConnectionInitEvent.class);
                                  });
                               */
                            },
                        BotConnectionInitEvent.class);
              });
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
            true);
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
  }

  private static class POVSessionState {
    private String connectedBot;
  }

  private static class POVServerInstance {
    public POVServerInstance(int port, AttackManager attackManager) {
      var pong =
          new ServerStatusInfo(
              new VersionInfo(
                  MinecraftCodec.CODEC.getMinecraftVersion(),
                  MinecraftCodec.CODEC.getProtocolVersion()),
              new PlayerInfo(100, 0, new ArrayList<>()),
              Component.text("Attack POV server for attack " + attackManager.id() + "!")
                  .color(NamedTextColor.GREEN)
                  .decorate(TextDecoration.BOLD),
              null,
              false);
      Server server = new TcpServer("0.0.0.0", port, MinecraftProtocol::new);

      server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, false);
      server.setGlobalFlag(
          MinecraftConstants.SERVER_INFO_BUILDER_KEY, (ServerInfoBuilder) session -> pong);

      server.setGlobalFlag(
          MinecraftConstants.SERVER_LOGIN_HANDLER_KEY,
          (ServerLoginHandler)
              session -> {
                session.send(
                    new ClientboundLoginPacket(
                        0,
                        false,
                        new String[] {"minecraft:the_end"},
                        1,
                        0,
                        0,
                        false,
                        false,
                        false,
                        new PlayerSpawnInfo(
                            "minecraft:the_end",
                            "minecraft:the_end",
                            100,
                            GameMode.SPECTATOR,
                            GameMode.SPECTATOR,
                            false,
                            false,
                            null,
                            100)));

                session.send(
                    new ClientboundPlayerAbilitiesPacket(false, false, true, false, 0f, 0f));

                // without this the player will spawn only after waiting 30 seconds
                // there are multiple options to fix that,
                // but this is the best option as we don't want to send chunk and the player is in
                // spectator anyway
                session.send(new ClientboundSetHealthPacket(0, 0, 0));

                // this packet is also required to let our player spawn, but the location itself
                // doesn't matter
                session.send(new ClientboundSetDefaultSpawnPositionPacket(Vector3i.ZERO, 0));

                // we have to listen to the teleport confirm on the PacketHandler to prevent respawn
                // request packet spam,
                // so send it after calling ConnectedEvent which adds the PacketHandler as listener
                session.send(new ClientboundPlayerPositionPacket(0, 0, 0, 0, 0, 0));

                // this packet is required since 1.20.3
                session.send(
                    new ClientboundGameEventPacket(GameEvent.LEVEL_CHUNKS_LOAD_START, null));

                // Manually call the connect event
                session.callEvent(new ConnectedEvent(session));
              });
      server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 256); // default

      server.addListener(
          new ServerAdapter() {
            @Override
            public void serverClosed(ServerClosedEvent event) {
              super.serverClosed(event);
            }

            @Override
            public void sessionAdded(SessionAddedEvent event) {
              var session = event.getSession();
              session.setFlag("pov-state", new POVSessionState());

              session.addListener(
                  new SessionAdapter() {
                    @Override
                    public void packetReceived(Session session, Packet packet) {
                      if (packet instanceof ServerboundChatPacket chatPacket) {
                        GameProfile profile =
                            event.getSession().getFlag(MinecraftConstants.PROFILE_KEY);

                        log.info("{}: {}", profile.getName(), chatPacket.getMessage());
                      } else if (packet instanceof ServerboundAcceptTeleportationPacket) {
                        // if we keep the health on 0, the client will spam us respawn request packets :/
                        session.send(new ClientboundSetHealthPacket(1, 0, 0));
                      }
                    }

                    @Override
                    public void connected(ConnectedEvent event) {
                      var session = event.getSession();
                      try {
                        GameProfile profile =
                            session.getFlag(MinecraftConstants.PROFILE_KEY);
                        log.info("Connected: {}", profile.getName());

                        Component msg =
                            Component.text("Hello, ")
                                .color(NamedTextColor.GREEN)
                                .append(
                                    Component.text(profile.getName())
                                        .color(NamedTextColor.AQUA)
                                        .decorate(TextDecoration.UNDERLINED))
                                .append(
                                    Component.text(
                                            "! To connect to the POV of a bot, please send the bot name as a chat message.")
                                        .color(NamedTextColor.GREEN));

                        session.send(new ClientboundSystemChatPacket(msg, false));
                      } catch (Exception e) {
                        log.error("Error sending welcome message", e);
                      }
                    }

                    @Override
                    public void packetError(PacketErrorEvent event) {
                      log.error("Packet error", event.getCause());
                    }

                    @Override
                    public void disconnecting(DisconnectingEvent event) {
                      log.info("Disconnecting: {}", event.getReason(), event.getCause());
                    }
                  });
            }

            @Override
            public void sessionRemoved(SessionRemovedEvent event) {
              log.info("Session removed.");
            }
          });

      server.bind();
    }
  }
}
