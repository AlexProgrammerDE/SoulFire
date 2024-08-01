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

import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.ServerCommandManager;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginHelper;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.EventUtil;
import com.soulfiremc.server.api.event.attack.AttackEndedEvent;
import com.soulfiremc.server.api.event.attack.AttackStartEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.SFProtocolConstants;
import com.soulfiremc.server.protocol.SFProtocolHelper;
import com.soulfiremc.server.protocol.bot.container.ContainerSlot;
import com.soulfiremc.server.protocol.bot.model.ChunkKey;
import com.soulfiremc.server.protocol.bot.state.LevelHeightAccessor;
import com.soulfiremc.server.protocol.bot.state.entity.ClientEntity;
import com.soulfiremc.server.protocol.bot.state.entity.ExperienceOrbEntity;
import com.soulfiremc.server.protocol.bot.state.entity.RawEntity;
import com.soulfiremc.server.protocol.bot.state.registry.SFChatType;
import com.soulfiremc.server.settings.BotSettings;
import com.soulfiremc.server.settings.lib.SettingsHolder;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.settings.property.StringProperty;
import com.soulfiremc.server.user.Permission;
import com.soulfiremc.server.user.ServerCommandSource;
import com.soulfiremc.server.util.TimeUtil;
import com.soulfiremc.util.PortHelper;
import com.soulfiremc.util.ResourceHelper;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.util.TriState;
import net.lenni0451.lambdaevents.EventHandler;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.server.ServerAdapter;
import org.geysermc.mcprotocollib.network.event.server.ServerClosedEvent;
import org.geysermc.mcprotocollib.network.event.server.SessionAddedEvent;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketErrorEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpServer;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntryAction;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeModifier;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.level.LightUpdateData;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEvent;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.RainStrengthValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.ThunderStrengthValue;
import org.geysermc.mcprotocollib.protocol.data.status.PlayerInfo;
import org.geysermc.mcprotocollib.protocol.data.status.ServerStatusInfo;
import org.geysermc.mcprotocollib.protocol.data.status.VersionInfo;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundPingPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundSelectKnownPacks;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundUpdateEnabledFeaturesPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundChangeDifficultyPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundStartConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundUpdateAttributesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundUpdateMobEffectPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetCarriedItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetExperiencePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddExperienceOrbPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetChunkCacheCenterPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;

@Slf4j
public class POVServer implements InternalPlugin {
  public static final PluginInfo PLUGIN_INFO = new PluginInfo(
    "pov-server",
    "1.0.0",
    "A plugin that allows users to control bots from a first-person perspective.",
    "AlexProgrammerDE",
    "GPL-3.0"
  );
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
  public static void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(POVServerSettings.class, "POV Server", PLUGIN_INFO);
  }

  private static GameProfile getFakePlayerListEntry(Component text) {
    return new GameProfile(UUID.randomUUID(), LegacyComponentSerializer.legacySection().serialize(text));
  }

  private static TcpServer startPOVServer(SettingsHolder settingsHolder, int port, InstanceManager instanceManager) {
    var faviconBytes = ResourceHelper.getResourceAsBytes("assets/pov_favicon.png");
    var server = new TcpServer("0.0.0.0", port, MinecraftProtocol::new);

    server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, false);
    server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, session -> new ServerStatusInfo(
      Component.text("Attack POV server for attack %s!".formatted(instanceManager.id()))
        .color(NamedTextColor.GREEN)
        .decorate(TextDecoration.BOLD),
      new PlayerInfo(settingsHolder.get(BotSettings.AMOUNT), instanceManager.botConnections().size(), List.of(
        getFakePlayerListEntry(Component.text("Observe and control bots!").color(NamedTextColor.GREEN)),
        getFakePlayerListEntry(Component.text("Play the server through the bots.").color(NamedTextColor.GREEN)),
        getFakePlayerListEntry(Component.text("Still experimental!").color(NamedTextColor.RED))
      )),
      new VersionInfo(
        MinecraftCodec.CODEC.getMinecraftVersion(),
        MinecraftCodec.CODEC.getProtocolVersion()),
      faviconBytes,
      false));

    server.setGlobalFlag(
      MinecraftConstants.SERVER_LOGIN_HANDLER_KEY,
      session -> {
        session.send(
          new ClientboundLoginPacket(
            0,
            false,
            new Key[] {Key.key("minecraft:the_end")},
            1,
            0,
            0,
            false,
            false,
            false,
            new PlayerSpawnInfo(
              2,
              Key.key("minecraft:the_end"),
              100,
              GameMode.SPECTATOR,
              GameMode.SPECTATOR,
              false,
              false,
              null,
              0),
            false));

        session.send(
          new ClientboundPlayerAbilitiesPacket(false, false, true, false, 0.05f, 0.1f));

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

        // End dimension height
        var heightAccessor = new LevelHeightAccessor() {
          @Override
          public int getHeight() {
            return 256;
          }

          @Override
          public int getMinBuildHeight() {
            return 0;
          }
        };
        var sectionCount = heightAccessor.getSectionsCount();
        var buf = Unpooled.buffer();
        for (var i = 0; i < sectionCount; i++) {
          var chunk = DataPalette.createForChunk();
          chunk.set(0, 0, 0, 0);
          var biome = DataPalette.createForBiome();
          biome.set(0, 0, 0, 0);
          SFProtocolHelper.writeChunkSection(
            buf,
            new ChunkSection(0, chunk, biome),
            (MinecraftCodecHelper) session.getCodecHelper());
        }

        var chunkBytes = new byte[buf.readableBytes()];
        buf.readBytes(chunkBytes);

        var lightMask = new BitSet();
        lightMask.set(0, sectionCount + 2);
        var skyUpdateList = new ArrayList<byte[]>();
        for (var i = 0; i < sectionCount + 2; i++) {
          skyUpdateList.add(FULL_LIGHT); // sky light
        }

        var lightUpdateData =
          new LightUpdateData(
            lightMask, new BitSet(), new BitSet(), lightMask, skyUpdateList, List.of());

        session.send(
          new ClientboundLevelChunkWithLightPacket(
            0,
            0,
            chunkBytes,
            NbtMap.EMPTY,
            new BlockEntityInfo[0],
            lightUpdateData));

        // Manually call the connect event
        session.callEvent(new ConnectedEvent(session));

        var brandBuffer = Unpooled.buffer();
        session.getCodecHelper().writeString(brandBuffer, "SoulFire POV");

        var brandBytes = new byte[brandBuffer.readableBytes()];
        brandBuffer.readBytes(brandBytes);

        session.send(
          new ClientboundCustomPayloadPacket(SFProtocolConstants.BRAND_PAYLOAD_KEY, brandBytes));
      });

    server.addListener(
      new ServerAdapter() {
        @Override
        public void serverClosed(ServerClosedEvent event) {
          super.serverClosed(event);
        }

        @Override
        public void sessionAdded(SessionAddedEvent event) {
          var session = event.getSession();

          session.addListener(
            new SessionAdapter() {
              private BotConnection botConnection;
              private boolean enableForwarding;
              private double lastX;
              private double lastY;
              private double lastZ;

              @Override
              public void packetSent(Session session, Packet packet) {
                log.debug("POV -> C: {}", packet.getClass().getSimpleName());
              }

              @Override
              public void packetReceived(Session session, Packet packet) {
                log.debug("C -> POV: {}", packet.getClass().getSimpleName());
                if (botConnection == null) {
                  if (packet instanceof ServerboundChatPacket chatPacket) {
                    var profile =
                      event.getSession().getFlag(MinecraftConstants.PROFILE_KEY);

                    var selectedName = chatPacket.getMessage();
                    log.info("{}: {}", profile.getName(), selectedName);

                    var first =
                      instanceManager.botConnections().values().stream()
                        .filter(c -> c.accountName().equals(selectedName))
                        .findFirst();
                    if (first.isEmpty()) {
                      session.send(
                        new ClientboundSystemChatPacket(
                          Component.text("Bot not found!").color(NamedTextColor.RED),
                          false));
                      return;
                    }

                    botConnection = first.get();
                    var povSession = session;
                    botConnection
                      .session()
                      .addListener(
                        new SessionAdapter() {
                          @Override
                          public void packetReceived(Session session, Packet packet) {
                            if (!enableForwarding
                              || NOT_SYNCED.contains(packet.getClass())) {
                              return;
                            }

                            if (packet instanceof ClientboundPlayerChatPacket chatPacket) {
                              // To avoid signature issues since the signature is for the bot, not the connected user
                              povSession.send(new ClientboundSystemChatPacket(botConnection.dataManager().prepareChatTypeMessage(chatPacket.getChatType(), new SFChatType.BoundChatMessageInfo(
                                botConnection.dataManager().getComponentForPlayerChat(chatPacket),
                                chatPacket.getName(),
                                chatPacket.getTargetName()
                              )), false));
                              return;
                            }

                            // MC Server of the bot -> MC Client
                            povSession.send(packet);
                          }

                          @Override
                          public void packetSent(Session session, Packet packet) {
                            if (!enableForwarding
                              || NOT_SYNCED.contains(packet.getClass())) {
                              return;
                            }

                            var clientEntity =
                              botConnection.dataManager().clientEntity();
                            // Bot -> MC Client
                            switch (packet) {
                              case ServerboundMovePlayerPosRotPacket posRot -> {
                                povSession.send(
                                  new ClientboundMoveEntityPosRotPacket(
                                    clientEntity.entityId(),
                                    (posRot.getX() * 32 - lastX * 32) * 128,
                                    (posRot.getY() * 32 - lastY * 32) * 128,
                                    (posRot.getZ() * 32 - lastZ * 32) * 128,
                                    posRot.getYaw(),
                                    posRot.getPitch(),
                                    clientEntity.onGround()));
                                lastX = posRot.getX();
                                lastY = posRot.getY();
                                lastZ = posRot.getZ();
                              }
                              case ServerboundMovePlayerPosPacket pos -> {
                                povSession.send(
                                  new ClientboundMoveEntityPosPacket(
                                    clientEntity.entityId(),
                                    (pos.getX() * 32 - lastX * 32) * 128,
                                    (pos.getY() * 32 - lastY * 32) * 128,
                                    (pos.getZ() * 32 - lastZ * 32) * 128,
                                    clientEntity.onGround()));
                                lastX = pos.getX();
                                lastY = pos.getY();
                                lastZ = pos.getZ();
                              }
                              case ServerboundMovePlayerRotPacket rot -> povSession.send(
                                new ClientboundMoveEntityRotPacket(
                                  clientEntity.entityId(),
                                  rot.getYaw(),
                                  rot.getPitch(),
                                  clientEntity.onGround()));
                              default -> {
                              }
                            }
                          }
                        });
                    Thread.ofPlatform()
                      .name("SyncTask")
                      .start(
                        () -> {
                          syncBotAndUser();
                          session.send(
                            new ClientboundSystemChatPacket(
                              Component.text("Connected to bot ")
                                .color(NamedTextColor.GREEN)
                                .append(
                                  Component.text(
                                      botConnection.accountName())
                                    .color(NamedTextColor.AQUA)
                                    .decorate(TextDecoration.UNDERLINED))
                                .append(Component.text("!"))
                                .color(NamedTextColor.GREEN),
                              false));
                        });
                  }
                } else if (enableForwarding && !NOT_SYNCED.contains(packet.getClass())) {
                  // For data consistence, ensure all packets sent from client -> server are
                  // handled on the bots tick event loop
                  botConnection.preTickHooks().add(() -> {
                    var clientEntity = botConnection.dataManager().clientEntity();
                    switch (packet) {
                      case ServerboundMovePlayerPosRotPacket posRot -> {
                        lastX = posRot.getX();
                        lastY = posRot.getY();
                        lastZ = posRot.getZ();

                        clientEntity.x(posRot.getX());
                        clientEntity.y(posRot.getY());
                        clientEntity.z(posRot.getZ());
                        clientEntity.yaw(posRot.getYaw());
                        clientEntity.pitch(posRot.getPitch());
                      }
                      case ServerboundMovePlayerPosPacket pos -> {
                        lastX = pos.getX();
                        lastY = pos.getY();
                        lastZ = pos.getZ();

                        clientEntity.x(pos.getX());
                        clientEntity.y(pos.getY());
                        clientEntity.z(pos.getZ());
                      }
                      case ServerboundMovePlayerRotPacket rot -> {
                        clientEntity.yaw(rot.getYaw());
                        clientEntity.pitch(rot.getPitch());
                      }
                      case ServerboundAcceptTeleportationPacket teleportationPacket -> {
                        // This was a forced teleport, the server should not know about it
                        if (teleportationPacket.getId() == Integer.MIN_VALUE) {
                          return;
                        }
                      }
                      case ServerboundChatPacket chatPacket -> {
                        if (settingsHolder.get(POVServerSettings.ENABLE_COMMANDS)) {
                          var message = chatPacket.getMessage();
                          var prefix = settingsHolder.get(POVServerSettings.COMMAND_PREFIX);
                          if (message.startsWith(prefix)) {
                            var command = message.substring(prefix.length());
                            var source = new PovServerUser(session, session.getFlag(MinecraftConstants.PROFILE_KEY).getName());
                            var code = instanceManager
                              .soulFireServer()
                              .injector()
                              .getSingleton(ServerCommandManager.class)
                              .execute(command, source);

                            log.info("Command \"%s\" executed! (Code: %d)".formatted(command, code));
                            return;
                          }
                        }
                      }
                      default -> {
                      }
                    }

                    // The client spams too many packets when being force-moved,
                    // so we'll just ignore them
                    if (clientEntity.controlState().isActivelyControlling()) {
                      return;
                    }

                    // MC Client -> Server of the bot
                    botConnection.session().send(packet);
                  });
                }
              }

              @Override
              public void connected(ConnectedEvent event) {
                if (botConnection == null) {
                  var session = event.getSession();
                  var profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
                  log.info("Account connected: {}", profile.getName());

                  Component msg =
                    Component.text("Hello, ")
                      .color(NamedTextColor.GREEN)
                      .append(
                        Component.text(profile.getName())
                          .color(NamedTextColor.AQUA)
                          .decorate(TextDecoration.UNDERLINED))
                      .append(
                        Component.text("! To connect to the POV of a bot, please send the bot name as a chat message.")
                          .color(NamedTextColor.GREEN));

                  session.send(new ClientboundSystemChatPacket(msg, false));
                }
              }

              @Override
              public void packetError(PacketErrorEvent event) {
                log.error("Packet error", event.getCause());
              }

              @Override
              public void disconnected(DisconnectedEvent event) {
                log.info("Disconnected: {}", SoulFireServer.PLAIN_MESSAGE_SERIALIZER.serialize(event.getReason()), event.getCause());
              }

              private void awaitReceived(Class<?> clazz) {
                var future = new CompletableFuture<Void>();

                session.addListener(
                  new SessionAdapter() {
                    @Override
                    public void packetReceived(Session session, Packet packet) {
                      if (clazz.isInstance(packet)) {
                        future.complete(null);
                      }
                    }
                  });

                future.orTimeout(30, TimeUnit.SECONDS).join();
              }

              private void syncBotAndUser() {
                Objects.requireNonNull(botConnection);
                var dataManager = botConnection.dataManager();

                session.send(new ClientboundStartConfigurationPacket(), () ->
                  ((MinecraftProtocol) session.getPacketProtocol()).setOutboundState(ProtocolState.CONFIGURATION));
                awaitReceived(ServerboundConfigurationAcknowledgedPacket.class);

                if (dataManager.serverEnabledFeatures() != null) {
                  session.send(
                    new ClientboundUpdateEnabledFeaturesPacket(
                      dataManager.serverEnabledFeatures()));
                }

                if (dataManager.serverKnownPacks() != null) {
                  session.send(new ClientboundSelectKnownPacks(dataManager.serverKnownPacks()));
                }

                for (var entry : dataManager.rawRegistryData().entrySet()) {
                  session.send(new ClientboundRegistryDataPacket(entry.getKey().key(), entry.getValue()));
                }

                var tagsPacket = new ClientboundUpdateTagsPacket();
                tagsPacket.getTags().putAll(dataManager.tagsState().exportTags());
                session.send(tagsPacket);

                session.send(new ClientboundFinishConfigurationPacket());
                awaitReceived(ServerboundFinishConfigurationPacket.class);

                var spawnInfo =
                  new PlayerSpawnInfo(
                    dataManager.currentLevel().dimensionType().id(),
                    dataManager.currentLevel().worldKey(),
                    dataManager.currentLevel().hashedSeed(),
                    dataManager.gameMode(),
                    dataManager.previousGameMode(),
                    dataManager.currentLevel().debug(),
                    dataManager.currentLevel().flat(),
                    dataManager.lastDeathPos(),
                    dataManager.portalCooldown());
                session.send(
                  new ClientboundLoginPacket(
                    dataManager.clientEntity().entityId(),
                    dataManager.loginData().hardcore(),
                    dataManager.loginData().worldNames(),
                    dataManager.loginData().maxPlayers(),
                    dataManager.serverViewDistance(),
                    dataManager.serverSimulationDistance(),
                    dataManager.clientEntity().showReducedDebug(),
                    dataManager.enableRespawnScreen(),
                    dataManager.doLimitedCrafting(),
                    spawnInfo,
                    dataManager.loginData().enforcesSecureChat()));
                session.send(new ClientboundRespawnPacket(spawnInfo, false, false));

                if (dataManager.difficultyData() != null) {
                  session.send(
                    new ClientboundChangeDifficultyPacket(
                      dataManager.difficultyData().difficulty(),
                      dataManager.difficultyData().locked()));
                }

                if (dataManager.abilitiesData() != null) {
                  session.send(
                    new ClientboundPlayerAbilitiesPacket(
                      dataManager.abilitiesData().invulnerable(),
                      dataManager.abilitiesData().flying(),
                      dataManager.abilitiesData().allowFlying(),
                      dataManager.abilitiesData().creativeModeBreak(),
                      dataManager.abilitiesData().flySpeed(),
                      dataManager.abilitiesData().walkSpeed()));
                }

                session.send(
                  new ClientboundGameEventPacket(
                    GameEvent.CHANGE_GAMEMODE, dataManager.gameMode()));

                if (dataManager.borderState() != null) {
                  session.send(
                    new ClientboundInitializeBorderPacket(
                      dataManager.borderState().centerX(),
                      dataManager.borderState().centerZ(),
                      dataManager.borderState().oldSize(),
                      dataManager.borderState().newSize(),
                      dataManager.borderState().lerpTime(),
                      dataManager.borderState().newAbsoluteMaxSize(),
                      dataManager.borderState().warningBlocks(),
                      dataManager.borderState().warningTime()));
                }

                if (dataManager.defaultSpawnData() != null) {
                  session.send(
                    new ClientboundSetDefaultSpawnPositionPacket(
                      dataManager.defaultSpawnData().position(),
                      dataManager.defaultSpawnData().angle()));
                }

                if (dataManager.weatherState() != null) {
                  session.send(
                    new ClientboundGameEventPacket(
                      dataManager.weatherState().raining()
                        ? GameEvent.START_RAIN
                        : GameEvent.STOP_RAIN,
                      null));
                  session.send(
                    new ClientboundGameEventPacket(
                      GameEvent.RAIN_STRENGTH,
                      new RainStrengthValue(
                        dataManager.weatherState().rainStrength())));
                  session.send(
                    new ClientboundGameEventPacket(
                      GameEvent.THUNDER_STRENGTH,
                      new ThunderStrengthValue(
                        dataManager.weatherState().thunderStrength())));
                }

                if (dataManager.healthData() != null) {
                  session.send(
                    new ClientboundSetHealthPacket(
                      dataManager.healthData().health(),
                      dataManager.healthData().food(),
                      dataManager.healthData().saturation()));
                }

                if (dataManager.experienceData() != null) {
                  session.send(
                    new ClientboundSetExperiencePacket(
                      dataManager.experienceData().experience(),
                      dataManager.experienceData().level(),
                      dataManager.experienceData().totalExperience()));
                }

                // Give initial coordinates to the client
                session.send(
                  new ClientboundPlayerPositionPacket(
                    dataManager.clientEntity().x(),
                    dataManager.clientEntity().y(),
                    dataManager.clientEntity().z(),
                    dataManager.clientEntity().yaw(),
                    dataManager.clientEntity().pitch(),
                    Integer.MIN_VALUE));

                if (dataManager.playerListState().header() != null
                  && dataManager.playerListState().footer() != null) {
                  session.send(
                    new ClientboundTabListPacket(
                      dataManager.playerListState().header(),
                      dataManager.playerListState().footer()));
                }

                var currentId =
                  session.getFlag(MinecraftConstants.PROFILE_KEY).getId();
                session.send(
                  new ClientboundPlayerInfoUpdatePacket(
                    EnumSet.of(
                      PlayerListEntryAction.ADD_PLAYER,
                      PlayerListEntryAction.INITIALIZE_CHAT,
                      PlayerListEntryAction.UPDATE_GAME_MODE,
                      PlayerListEntryAction.UPDATE_LISTED,
                      PlayerListEntryAction.UPDATE_LATENCY,
                      PlayerListEntryAction.UPDATE_DISPLAY_NAME),
                    dataManager.playerListState().entries().values().stream()
                      .map(
                        entry -> {
                          if (entry
                            .getProfileId()
                            .equals(dataManager.botProfile().getId())) {
                            GameProfile newGameProfile;
                            if (entry.getProfile() == null) {
                              newGameProfile = null;
                            } else {
                              newGameProfile =
                                new GameProfile(
                                  currentId, entry.getProfile().getName());
                              newGameProfile.setProperties(
                                entry.getProfile().getProperties());
                            }

                            return new PlayerListEntry(
                              currentId,
                              newGameProfile,
                              entry.isListed(),
                              entry.getLatency(),
                              entry.getGameMode(),
                              entry.getDisplayName(),
                              entry.getSessionId(),
                              entry.getExpiresAt(),
                              entry.getPublicKey(),
                              entry.getKeySignature());
                          } else {
                            return entry;
                          }
                        })
                      .toList()
                      .toArray(new PlayerListEntry[0])));

                if (dataManager.centerChunk() != null) {
                  session.send(
                    new ClientboundSetChunkCacheCenterPacket(
                      dataManager.centerChunk().chunkX(),
                      dataManager.centerChunk().chunkZ()));
                }

                session.send(
                  new ClientboundGameEventPacket(GameEvent.LEVEL_CHUNKS_LOAD_START, null));

                for (var chunkEntry :
                  dataManager.currentLevel()
                    .chunks()
                    .getChunks()
                    .long2ObjectEntrySet()) {
                  var chunkKey = ChunkKey.fromKey(chunkEntry.getLongKey());
                  var chunk = chunkEntry.getValue();
                  var buf = Unpooled.buffer();

                  for (var i = 0; i < chunk.getSectionCount(); i++) {
                    SFProtocolHelper.writeChunkSection(
                      buf,
                      chunk.getSection(i),
                      dataManager.codecHelper());
                  }

                  var chunkBytes = new byte[buf.readableBytes()];
                  buf.readBytes(chunkBytes);

                  var lightMask = new BitSet();
                  lightMask.set(0, chunk.getSectionCount() + 2);
                  var skyUpdateList = new ArrayList<byte[]>();
                  for (var i = 0; i < chunk.getSectionCount() + 2; i++) {
                    skyUpdateList.add(FULL_LIGHT); // sky light
                  }

                  var lightUpdateData =
                    new LightUpdateData(
                      lightMask,
                      new BitSet(),
                      new BitSet(),
                      lightMask,
                      skyUpdateList,
                      List.of());

                  session.send(
                    new ClientboundLevelChunkWithLightPacket(
                      chunkKey.chunkX(),
                      chunkKey.chunkZ(),
                      chunkBytes,
                      NbtMap.EMPTY,
                      new BlockEntityInfo[0],
                      lightUpdateData));
                }

                if (dataManager.inventoryManager() != null) {
                  session.send(
                    new ClientboundSetCarriedItemPacket(
                      dataManager.inventoryManager().heldItemSlot()));
                  var stateIndex = 0;
                  for (var container :
                    dataManager.inventoryManager().containerData().values()) {
                    session.send(
                      new ClientboundContainerSetContentPacket(
                        container.id(),
                        stateIndex++,
                        Arrays.stream(
                            dataManager
                              .inventoryManager()
                              .playerInventory()
                              .slots())
                          .map(ContainerSlot::item)
                          .toList()
                          .toArray(new ItemStack[0]),
                        dataManager.inventoryManager().cursorItem()));

                    if (container.properties() != null) {
                      for (var containerProperty : container.properties().int2IntEntrySet()) {
                        session.send(
                          new ClientboundContainerSetDataPacket(
                            container.id(),
                            containerProperty.getIntKey(),
                            containerProperty.getIntValue()));
                      }
                    }
                  }
                }

                for (var entity : dataManager.entityTrackerState().getEntities()) {
                  if (entity instanceof ClientEntity clientEntity) {
                    session.send(
                      new ClientboundEntityEventPacket(
                        clientEntity.entityId(),
                        switch (clientEntity.opPermissionLevel()) {
                          case 0 -> EntityEvent.PLAYER_OP_PERMISSION_LEVEL_0;
                          case 1 -> EntityEvent.PLAYER_OP_PERMISSION_LEVEL_1;
                          case 2 -> EntityEvent.PLAYER_OP_PERMISSION_LEVEL_2;
                          case 3 -> EntityEvent.PLAYER_OP_PERMISSION_LEVEL_3;
                          case 4 -> EntityEvent.PLAYER_OP_PERMISSION_LEVEL_4;
                          default -> throw new IllegalStateException(
                            "Unexpected value: "
                              + clientEntity.opPermissionLevel());
                        }));
                    session.send(
                      new ClientboundEntityEventPacket(
                        clientEntity.entityId(),
                        clientEntity.showReducedDebug()
                          ? EntityEvent.PLAYER_ENABLE_REDUCED_DEBUG
                          : EntityEvent.PLAYER_DISABLE_REDUCED_DEBUG));
                  } else if (entity instanceof RawEntity rawEntity) {
                    session.send(
                      new ClientboundAddEntityPacket(
                        entity.entityId(),
                        entity.uuid(),
                        EntityType.from(entity.entityType().id()),
                        rawEntity.data(),
                        entity.x(),
                        entity.y(),
                        entity.z(),
                        entity.yaw(),
                        entity.headYaw(),
                        entity.pitch(),
                        entity.motionX(),
                        entity.motionY(),
                        entity.motionZ()));
                  } else if (entity instanceof ExperienceOrbEntity experienceOrbEntity) {
                    session.send(
                      new ClientboundAddExperienceOrbPacket(
                        entity.entityId(),
                        entity.x(),
                        entity.y(),
                        entity.z(),
                        experienceOrbEntity.expValue()));
                  }

                  for (var effect : entity.effectState().effects().entrySet()) {
                    session.send(
                      new ClientboundUpdateMobEffectPacket(
                        entity.entityId(),
                        effect.getKey(),
                        effect.getValue().amplifier(),
                        effect.getValue().duration(),
                        effect.getValue().ambient(),
                        effect.getValue().showParticles(),
                        effect.getValue().showIcon(),
                        effect.getValue().blend()));
                  }

                  session.send(
                    new ClientboundSetEntityDataPacket(
                      entity.entityId(),
                      entity
                        .metadataState()
                        .metadataStore()
                        .values()
                        .toArray(new EntityMetadata<?, ?>[0])));

                  session.send(
                    new ClientboundUpdateAttributesPacket(
                      entity.entityId(),
                      entity.attributeState().attributeStore().values().stream()
                        .map(
                          attributeState ->
                            new Attribute(
                              new AttributeType() {
                                @Override
                                public Key getIdentifier() {
                                  return attributeState.type().key();
                                }

                                @Override
                                public int getId() {
                                  return attributeState.type().id();
                                }
                              },
                              attributeState.baseValue(),
                              attributeState.modifiers().values().stream()
                                .map(
                                  modifier ->
                                    new AttributeModifier(
                                      modifier.id(),
                                      modifier.amount(),
                                      switch (modifier.operation()) {
                                        case ADD_VALUE -> ModifierOperation.ADD;
                                        case ADD_MULTIPLIED_BASE -> ModifierOperation.ADD_MULTIPLIED_BASE;
                                        case ADD_MULTIPLIED_TOTAL -> ModifierOperation.ADD_MULTIPLIED_TOTAL;
                                      }))
                                .toList()))
                        .toList()));
                }

                // Give the client a few moments to process the packets
                TimeUtil.waitTime(2, TimeUnit.SECONDS);

                enableForwarding = true;
              }
            });
        }
      });

    server.bind();

    return server;
  }

  @Override
  public PluginInfo pluginInfo() {
    return PLUGIN_INFO;
  }

  @Override
  public void onServer(SoulFireServer soulFireServer) {
    soulFireServer.registerListeners(POVServer.class);
    PluginHelper.registerAttackEventConsumer(
      soulFireServer,
      AttackStartEvent.class,
      event -> {
        var attackManager = event.instanceManager();
        var settingsHolder = attackManager.settingsHolder();
        if (!settingsHolder.get(POVServerSettings.ENABLED)) {
          return;
        }

        var freePort =
          PortHelper.getAvailablePort(settingsHolder.get(POVServerSettings.PORT_START));
        var serverInstance = startPOVServer(settingsHolder, freePort, attackManager);
        log.info("Started POV server on 0.0.0.0:{} for attack {}", freePort, attackManager.id());

        EventUtil.runAndAssertChanged(attackManager.eventBus(), () -> PluginHelper.registerSafeEventConsumer(
          attackManager.eventBus(),
          AttackEndedEvent.class,
          e -> {
            log.info("Stopping POV server for attack {}", attackManager.id());
            serverInstance.close();
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
