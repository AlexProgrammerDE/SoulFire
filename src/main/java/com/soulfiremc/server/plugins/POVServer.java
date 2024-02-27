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
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.github.steveice10.mc.protocol.data.game.chunk.DataPalette;
import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeModifier;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.ModifierOperation;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerSpawnInfo;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.data.game.level.LightUpdateData;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityInfo;
import com.github.steveice10.mc.protocol.data.game.level.notify.GameEvent;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundPingPacket;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundPongPacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundUpdateEnabledFeaturesPacket;
import com.github.steveice10.mc.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChangeDifficultyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundStartConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundUpdateAttributesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundUpdateMobEffectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetCarriedItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetExperiencePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddExperienceOrbPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetChunkCacheCenterPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.ServerClosedEvent;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectingEvent;
import com.github.steveice10.packetlib.event.session.PacketErrorEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpServer;
import com.soulfiremc.server.AttackManager;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.attack.AttackInitEvent;
import com.soulfiremc.server.api.event.lifecycle.SettingsRegistryInitEvent;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.SessionDataManager;
import com.soulfiremc.server.protocol.bot.container.ContainerSlot;
import com.soulfiremc.server.protocol.bot.model.ChunkKey;
import com.soulfiremc.server.protocol.bot.state.entity.ClientEntity;
import com.soulfiremc.server.protocol.bot.state.entity.ExperienceOrbEntity;
import com.soulfiremc.server.protocol.bot.state.entity.RawEntity;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.lib.property.BooleanProperty;
import com.soulfiremc.server.settings.lib.property.IntProperty;
import com.soulfiremc.server.settings.lib.property.Property;
import com.soulfiremc.util.PortHelper;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
  private static final List<Class<?>> NOT_SYNCED =
      List.of(
          ClientboundKeepAlivePacket.class,
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

          var freePort =
              PortHelper.getAvailablePort(settingsHolder.get(POVServerSettings.PORT_START));
          new POVServerInstance(freePort, attackManager);
          log.info("Started POV server on 0.0.0.0:{} for attack {}", freePort, attackManager.id());
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
                            0)));

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

                var sectionCount = 16;
                var buf = Unpooled.buffer();
                for (var i = 0; i < sectionCount; i++) {
                  var chunk = DataPalette.createForChunk();
                  chunk.set(0, 0, 0, 0);
                  var biome = DataPalette.createForBiome(6);
                  biome.set(0, 0, 0, 0);
                  SessionDataManager.writeChunkSection(
                      buf,
                      (MinecraftCodecHelper) session.getCodecHelper(),
                      new ChunkSection(0, chunk, biome));
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
                        new CompoundTag(""),
                        new BlockEntityInfo[0],
                        lightUpdateData));

                // Manually call the connect event
                session.callEvent(new ConnectedEvent(session));

                var brandBuffer = Unpooled.buffer();
                session.getCodecHelper().writeString(brandBuffer, "SoulFire POV");

                var brandBytes = new byte[brandBuffer.readableBytes()];
                brandBuffer.readBytes(brandBytes);

                session.send(new ClientboundCustomPayloadPacket("minecraft:brand", brandBytes));
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

              session.addListener(
                  new SessionAdapter() {
                    private BotConnection botConnection;
                    private boolean enableForwarding;

                    @Override
                    public void packetReceived(Session session, Packet packet) {
                      if (botConnection == null) {
                        if (packet instanceof ServerboundChatPacket chatPacket) {
                          GameProfile profile =
                              event.getSession().getFlag(MinecraftConstants.PROFILE_KEY);

                          log.info("{}: {}", profile.getName(), chatPacket.getMessage());

                          var first =
                              attackManager.botConnections().values().stream()
                                  .filter(
                                      c ->
                                          c.meta()
                                              .minecraftAccount()
                                              .username()
                                              .equals(chatPacket.getMessage()))
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

                                      // MC Server of the bot -> MC Client
                                      povSession.send(packet);
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
                                                            botConnection
                                                                .meta()
                                                                .minecraftAccount()
                                                                .username())
                                                        .color(NamedTextColor.AQUA)
                                                        .decorate(TextDecoration.UNDERLINED))
                                                .append(Component.text("!"))
                                                .color(NamedTextColor.GREEN),
                                            false));
                                  });
                        }
                      } else if (!NOT_SYNCED.contains(packet.getClass()) && enableForwarding) {
                        // MC Client -> Server of the bot
                        botConnection.session().send(packet);
                      }
                    }

                    @Override
                    public void connected(ConnectedEvent event) {
                      if (botConnection == null) {
                        var session = event.getSession();
                        GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
                        log.info("Account connected: {}", profile.getName());

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
                      var sessionDataManager = botConnection.sessionDataManager();

                      session.send(new ClientboundStartConfigurationPacket());
                      awaitReceived(ServerboundConfigurationAcknowledgedPacket.class);
                      ((MinecraftProtocol) session.getPacketProtocol())
                          .setState(ProtocolState.CONFIGURATION);

                      session.send(
                          new ClientboundUpdateEnabledFeaturesPacket(
                              new String[] {"minecraft:vanilla"}));
                      session.send(
                          new ClientboundRegistryDataPacket(MinecraftProtocol.loadNetworkCodec()));
                      var tagsPacket = new ClientboundUpdateTagsPacket();
                      tagsPacket.getTags().putAll(sessionDataManager.tagsState().exportTags());

                      session.send(new ClientboundFinishConfigurationPacket());
                      awaitReceived(ServerboundFinishConfigurationPacket.class);

                      ((MinecraftProtocol) session.getPacketProtocol())
                          .setState(ProtocolState.GAME);

                      var spawnInfo =
                          new PlayerSpawnInfo(
                              sessionDataManager.currentDimension().dimensionType(),
                              sessionDataManager.currentDimension().worldName(),
                              sessionDataManager.currentDimension().hashedSeed(),
                              sessionDataManager.gameMode(),
                              sessionDataManager.previousGameMode(),
                              sessionDataManager.currentDimension().debug(),
                              sessionDataManager.currentDimension().flat(),
                              sessionDataManager.lastDeathPos(),
                              sessionDataManager.portalCooldown());
                      session.send(
                          new ClientboundLoginPacket(
                              sessionDataManager.clientEntity().entityId(),
                              sessionDataManager.loginData().hardcore(),
                              sessionDataManager.loginData().worldNames(),
                              sessionDataManager.loginData().maxPlayers(),
                              sessionDataManager.serverViewDistance(),
                              sessionDataManager.serverSimulationDistance(),
                              sessionDataManager.clientEntity().showReducedDebug(),
                              sessionDataManager.enableRespawnScreen(),
                              sessionDataManager.doLimitedCrafting(),
                              spawnInfo));
                      session.send(new ClientboundRespawnPacket(spawnInfo, false, false));

                      if (sessionDataManager.difficultyData() != null) {
                        session.send(
                            new ClientboundChangeDifficultyPacket(
                                sessionDataManager.difficultyData().difficulty(),
                                sessionDataManager.difficultyData().locked()));
                      }

                      if (sessionDataManager.abilitiesData() != null) {
                        session.send(
                            new ClientboundPlayerAbilitiesPacket(
                                sessionDataManager.abilitiesData().invulnerable(),
                                sessionDataManager.abilitiesData().flying(),
                                sessionDataManager.abilitiesData().allowFlying(),
                                sessionDataManager.abilitiesData().creativeModeBreak(),
                                sessionDataManager.abilitiesData().flySpeed(),
                                sessionDataManager.abilitiesData().walkSpeed()));
                      }

                      session.send(
                          new ClientboundGameEventPacket(
                              GameEvent.CHANGE_GAMEMODE, sessionDataManager.gameMode()));

                      if (sessionDataManager.borderState() != null) {
                        session.send(
                            new ClientboundInitializeBorderPacket(
                                sessionDataManager.borderState().centerX(),
                                sessionDataManager.borderState().centerZ(),
                                sessionDataManager.borderState().oldSize(),
                                sessionDataManager.borderState().newSize(),
                                sessionDataManager.borderState().lerpTime(),
                                sessionDataManager.borderState().newAbsoluteMaxSize(),
                                sessionDataManager.borderState().warningBlocks(),
                                sessionDataManager.borderState().warningTime()));
                      }

                      if (sessionDataManager.defaultSpawnData() != null) {
                        session.send(
                            new ClientboundSetDefaultSpawnPositionPacket(
                                sessionDataManager.defaultSpawnData().position(),
                                sessionDataManager.defaultSpawnData().angle()));
                      }

                      if (sessionDataManager.healthData() != null) {
                        session.send(
                            new ClientboundSetHealthPacket(
                                sessionDataManager.healthData().health(),
                                sessionDataManager.healthData().food(),
                                sessionDataManager.healthData().saturation()));
                      }

                      if (sessionDataManager.experienceData() != null) {
                        session.send(
                            new ClientboundSetExperiencePacket(
                                sessionDataManager.experienceData().experience(),
                                sessionDataManager.experienceData().level(),
                                sessionDataManager.experienceData().totalExperience()));
                      }

                      session.send(
                          new ClientboundPlayerPositionPacket(
                              sessionDataManager.clientEntity().x(),
                              sessionDataManager.clientEntity().y(),
                              sessionDataManager.clientEntity().z(),
                              sessionDataManager.clientEntity().yaw(),
                              sessionDataManager.clientEntity().pitch(),
                              Integer.MIN_VALUE));

                      if (sessionDataManager.playerListState().header() != null
                          && sessionDataManager.playerListState().footer() != null) {
                        session.send(
                            new ClientboundTabListPacket(
                                sessionDataManager.playerListState().header(),
                                sessionDataManager.playerListState().footer()));
                      }

                      var currentId =
                          session.<GameProfile>getFlag(MinecraftConstants.PROFILE_KEY).getId();
                      session.send(
                          new ClientboundPlayerInfoUpdatePacket(
                              EnumSet.of(
                                  PlayerListEntryAction.ADD_PLAYER,
                                  PlayerListEntryAction.INITIALIZE_CHAT,
                                  PlayerListEntryAction.UPDATE_GAME_MODE,
                                  PlayerListEntryAction.UPDATE_LISTED,
                                  PlayerListEntryAction.UPDATE_LATENCY,
                                  PlayerListEntryAction.UPDATE_DISPLAY_NAME),
                              sessionDataManager.playerListState().entries().values().stream()
                                  .map(
                                      entry -> {
                                        if (entry
                                            .getProfileId()
                                            .equals(sessionDataManager.botProfile().getId())) {
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

                      if (sessionDataManager.centerChunk() != null) {
                        session.send(
                            new ClientboundSetChunkCacheCenterPacket(
                                sessionDataManager.centerChunk().chunkX(),
                                sessionDataManager.centerChunk().chunkZ()));
                      }

                      session.send(
                          new ClientboundGameEventPacket(GameEvent.LEVEL_CHUNKS_LOAD_START, null));

                      for (var chunkEntry :
                          Objects.requireNonNull(sessionDataManager.getCurrentLevel())
                              .chunks()
                              .getChunks()
                              .long2ObjectEntrySet()) {
                        var chunkKey = ChunkKey.fromKey(chunkEntry.getLongKey());
                        var chunk = chunkEntry.getValue();
                        var buf = Unpooled.buffer();

                        for (var i = 0; i < chunk.getSectionCount(); i++) {
                          SessionDataManager.writeChunkSection(
                              buf,
                              sessionDataManager.session().getCodecHelper(),
                              chunk.getSection(i));
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
                                new CompoundTag(""),
                                new BlockEntityInfo[0],
                                lightUpdateData));
                      }

                      if (sessionDataManager.inventoryManager() != null) {
                        session.send(
                            new ClientboundSetCarriedItemPacket(
                                sessionDataManager.inventoryManager().heldItemSlot()));
                        var stateIndex = 0;
                        for (var container :
                            sessionDataManager.inventoryManager().containerData().values()) {
                          session.send(
                              new ClientboundContainerSetContentPacket(
                                  container.id(),
                                  ++stateIndex,
                                  Arrays.stream(
                                          sessionDataManager
                                              .inventoryManager()
                                              .playerInventory()
                                              .slots())
                                      .map(ContainerSlot::item)
                                      .toList()
                                      .toArray(new ItemStack[0]),
                                  sessionDataManager.inventoryManager().cursorItem()));

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

                      for (var entity : sessionDataManager.entityTrackerState().getEntities()) {
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
                                    default ->
                                        throw new IllegalStateException(
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
                                  effect.getValue().factorData()));
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
                                                new AttributeType.Custom(
                                                    "minecraft:" + attributeState.type().name()),
                                                attributeState.baseValue(),
                                                attributeState.modifiers().values().stream()
                                                    .map(
                                                        modifier ->
                                                            new AttributeModifier(
                                                                modifier.uuid(),
                                                                modifier.amount(),
                                                                switch (modifier.operation()) {
                                                                  case ADDITION ->
                                                                      ModifierOperation.ADD;
                                                                  case MULTIPLY_BASE ->
                                                                      ModifierOperation
                                                                          .ADD_MULTIPLIED;
                                                                  case MULTIPLY_TOTAL ->
                                                                      ModifierOperation.MULTIPLY;
                                                                }))
                                                    .toList()))
                                    .toList()));
                      }

                      // enableForwarding = true;
                    }
                  });
            }
          });

      server.bind();
    }
  }
}
