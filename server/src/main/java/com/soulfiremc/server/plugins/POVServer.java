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
import com.soulfiremc.server.adventure.SoulFireAdventure;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.attack.AttackEndedEvent;
import com.soulfiremc.server.api.event.attack.AttackStartEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.api.metadata.MetadataKey;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.BuiltInKnownPackRegistry;
import com.soulfiremc.server.protocol.SFProtocolConstants;
import com.soulfiremc.server.protocol.bot.container.ContainerSlot;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.container.WindowContainer;
import com.soulfiremc.server.protocol.bot.model.ChunkKey;
import com.soulfiremc.server.protocol.bot.state.LevelHeightAccessor;
import com.soulfiremc.server.protocol.bot.state.entity.Entity;
import com.soulfiremc.server.protocol.bot.state.entity.ExperienceOrbEntity;
import com.soulfiremc.server.protocol.bot.state.entity.LivingEntity;
import com.soulfiremc.server.protocol.bot.state.entity.Player;
import com.soulfiremc.server.protocol.bot.state.registry.SFChatType;
import com.soulfiremc.server.protocol.netty.ViaServer;
import com.soulfiremc.server.settings.instance.BotSettings;
import com.soulfiremc.server.settings.lib.InstanceSettingsSource;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableBooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableIntProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import com.soulfiremc.server.util.PortHelper;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.TimeUtil;
import com.soulfiremc.server.util.structs.DefaultTagsState;
import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.lenni0451.lambdaevents.EventHandler;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.Server;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.server.*;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketErrorEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.ServerLoginHandler;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntryAction;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.level.LightUpdateData;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.data.game.level.map.MapData;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEvent;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.RainStrengthValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.ThunderStrengthValue;
import org.geysermc.mcprotocollib.protocol.data.status.PlayerInfo;
import org.geysermc.mcprotocollib.protocol.data.status.ServerStatusInfo;
import org.geysermc.mcprotocollib.protocol.data.status.VersionInfo;
import org.geysermc.mcprotocollib.protocol.data.status.handler.ServerInfoBuilder;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundPingPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundSelectKnownPacks;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundUpdateEnabledFeaturesPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundSelectKnownPacks;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundUpdateAttributesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundUpdateMobEffectPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddExperienceOrbPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientTickEndPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import org.pf4j.Extension;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Extension
public final class POVServer extends InternalPlugin {
  private static final MetadataKey<Server> TCP_SERVER = MetadataKey.of("pov_server", "tcp_server", Server.class);
  private static final byte[] FULL_LIGHT = new byte[2048];

  static {
    Arrays.fill(FULL_LIGHT, (byte) 0xFF);
  }

  public POVServer() {
    super(new PluginInfo(
      "pov-server",
      "1.0.0",
      "A plugin that allows users to control bots from a first-person perspective.",
      "AlexProgrammerDE",
      "GPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  private static GameProfile getFakePlayerListEntry(Component text) {
    return new GameProfile(UUID.randomUUID(), LegacyComponentSerializer.legacySection().serialize(text));
  }

  private static void startPOVServer(InstanceSettingsSource settingsSource, int port, InstanceManager instanceManager) {
    var server = new ViaServer(new InetSocketAddress(port), MinecraftProtocol::new, instanceManager);

    server.setGlobalFlag(MinecraftConstants.SHOULD_AUTHENTICATE, false);
    server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, new POVServerInfoHandler(
      instanceManager,
      settingsSource,
      SFHelpers.getResourceAsBytes("icons/pov_favicon.png")
    ));
    server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, new POVServerLoginHandler());

    server.addListener(new POVServerAdapter(port, instanceManager));

    server.bind();
  }

  @SuppressWarnings("SameParameterValue")
  private static <T> CompletableFuture<T> awaitReceived(Session session, Class<T> clazz) {
    var future = new CompletableFuture<T>();

    session.addListener(
      new SessionAdapter() {
        @Override
        public void packetReceived(Session session, Packet packet) {
          if (clazz.isInstance(packet)) {
            future.complete(clazz.cast(packet));
          }
        }
      });

    return future.orTimeout(30, TimeUnit.SECONDS);
  }

  private static void syncBotToUser(BotConnection botConnection, Session clientSession) {
    Objects.requireNonNull(botConnection);
    var protocol = clientSession.getPacketProtocol();
    var dataManager = botConnection.dataManager();

    clientSession.send(new ClientboundStartConfigurationPacket());
    clientSession.switchOutboundState(() -> protocol.setOutboundState(ProtocolState.CONFIGURATION));
    TimeUtil.waitCondition(() -> protocol.getInboundState() != ProtocolState.CONFIGURATION, Duration.ofSeconds(30));

    clientSession.send(
      new ClientboundUpdateEnabledFeaturesPacket(
        dataManager.serverEnabledFeatures()));

    var clientPacks = awaitReceived(clientSession, ServerboundSelectKnownPacks.class);
    clientSession.send(new ClientboundSelectKnownPacks(dataManager.serverKnownPacks()));

    for (var entry : dataManager.registriesState().resolvedRegistryData().entrySet()) {
      var registryKey = entry.getKey();

      var sentEntries = new ArrayList<RegistryEntry>();
      for (var value : entry.getValue()) {
        var holderKey = value.getId();
        var serverHolderData = value.getData();
        var packData = BuiltInKnownPackRegistry.INSTANCE.findDataOptionally(registryKey, holderKey, clientPacks.join().getKnownPacks());

        RegistryEntry entryToSend;
        if (packData.isPresent() && packData.get().equals(serverHolderData)) {
          entryToSend = new RegistryEntry(holderKey, null);
        } else {
          entryToSend = new RegistryEntry(holderKey, serverHolderData);
        }

        sentEntries.add(entryToSend);
      }

      clientSession.send(new ClientboundRegistryDataPacket(registryKey.key(), sentEntries));
    }

    var tagsPacket = new ClientboundUpdateTagsPacket();
    tagsPacket.getTags().putAll(dataManager.tagsState().exportTags());
    clientSession.send(tagsPacket);

    clientSession.send(new ClientboundFinishConfigurationPacket());
    TimeUtil.waitCondition(() -> protocol.getInboundState() != ProtocolState.GAME, Duration.ofSeconds(30));

    var spawnInfo =
      new PlayerSpawnInfo(
        dataManager.currentLevel().dimensionType().id(),
        dataManager.currentLevel().worldKey(),
        dataManager.currentLevel().hashedSeed(),
        dataManager.gameModeState().localPlayerMode(),
        dataManager.gameModeState().previousLocalPlayerMode(),
        dataManager.currentLevel().debug(),
        dataManager.currentLevel().levelData().isFlat(),
        dataManager.localPlayer().getLastDeathLocation().orElse(null),
        dataManager.localPlayer().portalCooldown(),
        dataManager.currentLevel().seaLevel());
    clientSession.send(
      new ClientboundLoginPacket(
        dataManager.localPlayer().entityId(),
        dataManager.currentLevel().levelData().hardcore(),
        dataManager.levelNames(),
        dataManager.maxPlayers(),
        dataManager.serverChunkRadius(),
        dataManager.serverSimulationDistance(),
        dataManager.localPlayer().isReducedDebugInfo(),
        dataManager.localPlayer().shouldShowDeathScreen(),
        dataManager.localPlayer().getDoLimitedCrafting(),
        spawnInfo,
        dataManager.serverEnforcesSecureChat()));
    clientSession.send(new ClientboundRespawnPacket(spawnInfo, false, false));

    clientSession.send(
      new ClientboundChangeDifficultyPacket(
        dataManager.currentLevel().levelData().difficulty(),
        dataManager.currentLevel().levelData().difficultyLocked()));

    clientSession.send(
      new ClientboundSetDefaultSpawnPositionPacket(
        dataManager.currentLevel().levelData().spawnPos(),
        dataManager.currentLevel().levelData().spawnAngle()));

    clientSession.send(
      new ClientboundGameEventPacket(
        dataManager.currentLevel().levelData().raining()
          ? GameEvent.START_RAIN
          : GameEvent.STOP_RAIN,
        null));
    clientSession.send(
      new ClientboundGameEventPacket(
        GameEvent.RAIN_STRENGTH,
        new RainStrengthValue(
          dataManager.currentLevel().rainLevel())));
    clientSession.send(
      new ClientboundGameEventPacket(
        GameEvent.THUNDER_STRENGTH,
        new ThunderStrengthValue(
          dataManager.currentLevel().thunderLevel())));
    clientSession.send(
      new ClientboundGameEventPacket(
        GameEvent.CHANGE_GAMEMODE, dataManager.gameModeState().localPlayerMode()));

    // Should be after change CHANGE_GAMEMODE because setting GameMode overrides abilities
    var abilitiesData = dataManager.localPlayer().abilitiesState();
    clientSession.send(
      new ClientboundPlayerAbilitiesPacket(
        abilitiesData.invulnerable(),
        abilitiesData.mayfly(),
        abilitiesData.flying(),
        abilitiesData.instabuild(),
        abilitiesData.flySpeed(),
        abilitiesData.walkSpeed()));

    var borderState = dataManager.currentLevel().borderState();
    clientSession.send(
      new ClientboundInitializeBorderPacket(
        borderState.centerX(),
        borderState.centerZ(),
        borderState.oldSize(),
        borderState.newSize(),
        borderState.lerpTime(),
        borderState.newAbsoluteMaxSize(),
        borderState.warningBlocks(),
        borderState.warningTime()));

    var serverData = dataManager.serverPlayData();
    if (serverData != null) {
      clientSession.send(new ClientboundServerDataPacket(serverData.motd(), serverData.iconBytes()));
    }

    var commandsPacket = dataManager.commandsPacket();
    if (commandsPacket != null) {
      clientSession.send(commandsPacket);
    }

    // Give initial coordinates to the client
    clientSession.send(
      new ClientboundPlayerPositionPacket(
        Integer.MIN_VALUE,
        dataManager.localPlayer().pos(),
        dataManager.localPlayer().deltaMovement(),
        dataManager.localPlayer().yRot(),
        dataManager.localPlayer().xRot(),
        List.of()));

    // Send maps
    for (var entry : dataManager.mapDataStates().int2ObjectEntrySet()) {
      clientSession.send(
        new ClientboundMapItemDataPacket(
          entry.getIntKey(),
          entry.getValue().scale(),
          entry.getValue().locked(),
          entry.getValue().icons(),
          new MapData(128, 128, 0, 0, entry.getValue().colorData())));
    }

    var tabListHeader = dataManager.playerListState().header();
    var tabListFooter = dataManager.playerListState().footer();
    clientSession.send(
      new ClientboundTabListPacket(
        tabListHeader == null ? Component.empty() : tabListHeader,
        tabListFooter == null ? Component.empty() : tabListFooter));

    var originalClientId =
      clientSession.getFlag(MinecraftConstants.PROFILE_KEY).getId();
    clientSession.send(
      new ClientboundPlayerInfoUpdatePacket(
        EnumSet.allOf(PlayerListEntryAction.class),
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
                      originalClientId, entry.getProfile().getName());
                  newGameProfile.setProperties(
                    entry.getProfile().getProperties());
                }

                return new PlayerListEntry(
                  originalClientId,
                  newGameProfile,
                  entry.isListed(),
                  entry.getLatency(),
                  entry.getGameMode(),
                  entry.getDisplayName(),
                  entry.isShowHat(),
                  entry.getListOrder(),
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
      clientSession.send(
        new ClientboundSetChunkCacheCenterPacket(
          dataManager.centerChunk().chunkX(),
          dataManager.centerChunk().chunkZ()));
    }

    clientSession.send(
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
        MinecraftTypes.writeChunkSection(buf, chunk.getSection(i));
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

      clientSession.send(
        new ClientboundLevelChunkWithLightPacket(
          chunkKey.chunkX(),
          chunkKey.chunkZ(),
          chunkBytes,
          NbtMap.EMPTY,
          new BlockEntityInfo[0],
          lightUpdateData));
    }

    var localPlayer = dataManager.localPlayer();
    clientSession.send(new ClientboundSetHeldSlotPacket(localPlayer.inventory().selected));
    if (localPlayer.currentContainer instanceof WindowContainer windowContainer) {
      clientSession.send(
        new ClientboundOpenScreenPacket(
          windowContainer.containerId(),
          windowContainer.containerType(),
          windowContainer.title()));
    }
    var lastStateId = localPlayer.currentContainer.getStateId();
    Stream.of(localPlayer.inventoryMenu, localPlayer.currentContainer).distinct().forEach(container -> {
      clientSession.send(
        new ClientboundContainerSetContentPacket(
          container.containerId(),
          lastStateId,
          Arrays.stream(container.slots())
            .map(ContainerSlot::item)
            .map(SFItemStack::toMCPL)
            .toList()
            .toArray(new ItemStack[0]),
          container.getCarried().toMCPL()));

      for (var propertyKey : container.propertyKeys()) {
        clientSession.send(
          new ClientboundContainerSetDataPacket(
            container.containerId(),
            propertyKey,
            container.getProperty(propertyKey)));
      }
    });

    for (var entity : dataManager.currentLevel().getEntities()) {
      if (entity instanceof ExperienceOrbEntity experienceOrbEntity) {
        clientSession.send(
          new ClientboundAddExperienceOrbPacket(
            entity.entityId(),
            entity.x(),
            entity.y(),
            entity.z(),
            experienceOrbEntity.expValue()));
      } else if (entity instanceof Entity rawEntity && rawEntity.entityId() != dataManager.localPlayer().entityId()) {
        clientSession.send(
          new ClientboundAddEntityPacket(
            entity.entityId(),
            entity.uuid(),
            EntityType.from(entity.entityType().id()),
            rawEntity.data(),
            entity.x(),
            entity.y(),
            entity.z(),
            entity.yRot(),
            entity.headYRot(),
            entity.xRot(),
            entity.deltaMovement().getX(),
            entity.deltaMovement().getY(),
            entity.deltaMovement().getZ()));
      }

      if (entity instanceof Player player) {
        clientSession.send(
          new ClientboundEntityEventPacket(
            player.entityId(),
            switch (player.permissionLevel()) {
              case 0 -> EntityEvent.PLAYER_OP_PERMISSION_LEVEL_0;
              case 1 -> EntityEvent.PLAYER_OP_PERMISSION_LEVEL_1;
              case 2 -> EntityEvent.PLAYER_OP_PERMISSION_LEVEL_2;
              case 3 -> EntityEvent.PLAYER_OP_PERMISSION_LEVEL_3;
              case 4 -> EntityEvent.PLAYER_OP_PERMISSION_LEVEL_4;
              default -> throw new IllegalStateException(
                "Unexpected value: "
                  + player.permissionLevel());
            }));
        clientSession.send(
          new ClientboundSetExperiencePacket(
            player.experienceProgress,
            player.experienceLevel,
            player.totalExperience));
        clientSession.send(
          new ClientboundEntityEventPacket(
            player.entityId(),
            player.isReducedDebugInfo()
              ? EntityEvent.PLAYER_ENABLE_REDUCED_DEBUG
              : EntityEvent.PLAYER_DISABLE_REDUCED_DEBUG));
        clientSession.send(
          new ClientboundSetHealthPacket(
            player.getHealth(),
            player.getFoodData().getFoodLevel(),
            player.getFoodData().getSaturationLevel()));
      }

      for (var effect : entity.effectState().effects().entrySet()) {
        clientSession.send(
          new ClientboundUpdateMobEffectPacket(
            entity.entityId(),
            Effect.from(effect.getKey().id()),
            effect.getValue().amplifier(),
            effect.getValue().duration(),
            effect.getValue().ambient(),
            effect.getValue().showParticles(),
            effect.getValue().showIcon(),
            effect.getValue().blend()));
      }

      if (!entity.entityData().metadataStore().isEmpty()) {
        clientSession.send(
          new ClientboundSetEntityDataPacket(
            entity.entityId(),
            entity
              .entityData()
              .metadataStore()
              .values()
              .toArray(new EntityMetadata<?, ?>[0])));
      }

      if (entity instanceof LivingEntity livingEntity
        && !livingEntity.attributeState().attributeStore().isEmpty()) {
        clientSession.send(
          new ClientboundUpdateAttributesPacket(
            livingEntity.entityId(),
            livingEntity.attributeState().attributeStore().values().stream()
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
                    List.copyOf(attributeState.modifiers().values())))
              .toList()));
      }
    }
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addPluginPage(POVServerSettings.class, "POV Server", this, "view", POVServerSettings.ENABLED);
  }

  @EventHandler
  public void onAttackStart(AttackStartEvent event) {
    var instanceManager = event.instanceManager();
    var settingsSource = instanceManager.settingsSource();
    if (!settingsSource.get(POVServerSettings.ENABLED)) {
      return;
    }

    var freePort =
      PortHelper.getAvailablePort(settingsSource.get(POVServerSettings.PORT_START));
    startPOVServer(settingsSource, freePort, instanceManager);
  }

  @EventHandler
  public void onAttackEnded(AttackEndedEvent event) {
    var instanceManager = event.instanceManager();
    var currentInstance = instanceManager.metadata().getAndRemove(TCP_SERVER);
    if (currentInstance == null) {
      return;
    }

    log.info("Stopping POV server for instance {}", instanceManager.friendlyNameCache().get());
    currentInstance.close();
  }

  private record POVServerInfoHandler(InstanceManager instanceManager, InstanceSettingsSource settingsSource, byte[] faviconBytes) implements ServerInfoBuilder {
    @Override
    public ServerStatusInfo buildInfo(Session session) {
      var friendlyName = instanceManager.friendlyNameCache().get();
      return
        new ServerStatusInfo(
          Component.text("POV server for instance %s!".formatted(friendlyName))
            .color(NamedTextColor.GREEN),
          new PlayerInfo(settingsSource.get(BotSettings.AMOUNT), instanceManager.getConnectedBots().size(), List.of(
            getFakePlayerListEntry(Component.text("Observe and control bots!").color(NamedTextColor.GREEN)),
            getFakePlayerListEntry(Component.text("Play the server through the bots.").color(NamedTextColor.GREEN)),
            getFakePlayerListEntry(Component.text("Still experimental!").color(NamedTextColor.RED))
          )),
          new VersionInfo(
            MinecraftCodec.CODEC.getMinecraftVersion(),
            MinecraftCodec.CODEC.getProtocolVersion()),
          faviconBytes,
          false);
    }
  }

  private record POVServerLoginHandler() implements ServerLoginHandler {
    @Override
    public void loggedIn(Session session) {
      session.send(
        new ClientboundLoginPacket(
          0,
          false,
          new Key[]{Key.key("minecraft:the_end")},
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
            0,
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
      session.send(new ClientboundPlayerPositionPacket(0, Vector3d.ZERO, Vector3d.ZERO, 0, 0, List.of()));

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
        public int getMinY() {
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
        MinecraftTypes.writeChunkSection(buf, new ChunkSection(0, chunk, biome));
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

      var brandBuffer = Unpooled.buffer();
      MinecraftTypes.writeString(brandBuffer, "SoulFire POV");

      var brandBytes = new byte[brandBuffer.readableBytes()];
      brandBuffer.readBytes(brandBytes);

      session.send(
        new ClientboundCustomPayloadPacket(SFProtocolConstants.BRAND_PAYLOAD_KEY, brandBytes));

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

  @RequiredArgsConstructor
  private static class POVServerAdapter extends ServerAdapter {
    private final int port;
    private final InstanceManager instanceManager;

    @Override
    public void serverBound(ServerBoundEvent event) {
      var server = event.getServer();
      log.info("Started POV server on 0.0.0.0:{} for instance {}", port, instanceManager.friendlyNameCache().get());

      instanceManager.metadata().set(TCP_SERVER, server);
    }

    @Override
    public void serverClosing(ServerClosingEvent event) {
      log.info("POV server closing");
    }

    @Override
    public void serverClosed(ServerClosedEvent event) {
      log.info("POV server closed");
    }

    @Override
    public void sessionAdded(SessionAddedEvent event) {
      event.getSession().addListener(new C2POVAdapter(instanceManager));
      event.getSession().addListener(new SessionAdapter() {
        private boolean fixedOnce;

        @Override
        public void packetSending(PacketSendingEvent event) {
          if (fixedOnce) {
            return;
          }

          if (event.getPacket() instanceof ClientboundFinishConfigurationPacket) {
            fixedOnce = true;

            event.setCancelled(true);
            var tagsPacket = new ClientboundUpdateTagsPacket();
            tagsPacket.getTags().putAll(DefaultTagsState.TAGS_STATE.exportTags());
            event.getSession().send(tagsPacket);
            event.getSession().send(new ClientboundFinishConfigurationPacket());
          }
        }
      });
    }

    @Override
    public void sessionRemoved(SessionRemovedEvent event) {
      log.info("POV session removed");
    }
  }

  @RequiredArgsConstructor
  private static class C2POVAdapter extends SessionAdapter {
    private final InstanceManager instanceManager;
    private BotConnection botConnection;
    private boolean enableForwarding;
    private Vector3d lastPosition;
    private ServerboundClientInformationPacket lastClientSettings;

    @Override
    public void packetSent(Session clientSession, Packet packet) {
      log.trace("POV -> C: {}", packet.getClass().getSimpleName());
    }

    @Override
    public void packetReceived(Session clientSession, Packet packet) {
      log.trace("USER -> POV: {}", packet.getClass().getSimpleName());
      if (packet instanceof ServerboundClientInformationPacket clientSettings) {
        lastClientSettings = clientSettings;
        return;
      }

      if (botConnection == null) {
        if (packet instanceof ServerboundChatPacket chatPacket) {
          var profile =
            clientSession.getFlag(MinecraftConstants.PROFILE_KEY);

          var selectedName = chatPacket.getMessage();
          log.info("{}: {}", profile.getName(), selectedName);

          var first =
            instanceManager.getConnectedBots().stream()
              .filter(c -> c.accountName().equalsIgnoreCase(selectedName))
              .findFirst();
          if (first.isEmpty()) {
            clientSession.send(
              new ClientboundSystemChatPacket(
                Component.text("Bot not found!").color(NamedTextColor.RED),
                false));
            return;
          }

          botConnection = first.get();
          botConnection.session().addListener(new B2SAdapter(clientSession));
          botConnection.scheduler().schedule(() -> executeSync(clientSession));
        }
      } else if (enableForwarding) {
        // For data consistence, ensure all packets sent from client -> server are
        // handled on the bots tick event loop
        botConnection.preTickHooks().add(() -> {
          var clientEntity = botConnection.dataManager().localPlayer();
          switch (packet) {
            case ServerboundKeepAlivePacket ignored -> {
            }
            case ServerboundPongPacket ignored -> {
            }
            case ServerboundFinishConfigurationPacket ignored -> {
            }
            case ServerboundConfigurationAcknowledgedPacket ignored -> {
            }
            case ServerboundClientTickEndPacket ignored -> {
            }
            case ServerboundMovePlayerPosRotPacket posRot -> {
              lastPosition = Vector3d.from(posRot.getX(), posRot.getY(), posRot.getZ());

              clientEntity.setPos(posRot.getX(), posRot.getY(), posRot.getZ());
              clientEntity.setYRot(posRot.getYaw());
              clientEntity.setXRot(posRot.getPitch());
            }
            case ServerboundMovePlayerPosPacket pos -> {
              lastPosition = Vector3d.from(pos.getX(), pos.getY(), pos.getZ());

              clientEntity.setPos(pos.getX(), pos.getY(), pos.getZ());
            }
            case ServerboundMovePlayerRotPacket rot -> {
              clientEntity.setYRot(rot.getYaw());
              clientEntity.setXRot(rot.getPitch());
            }
            case ServerboundAcceptTeleportationPacket teleportationPacket -> {
              // This was a sync teleport, the server should not know about it
              if (teleportationPacket.getId() == Integer.MIN_VALUE) {
                return;
              }

              // MC Client -> Server of the bot
              botConnection.session().send(packet);
            }
            default -> // MC Client -> Server of the bot
              botConnection.session().send(packet);
          }
        });
      }
    }

    @Override
    public void packetError(PacketErrorEvent event) {
      log.error("POV -> USER Packet error", event.getCause());
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
      log.atInfo()
        .setCause(event.getCause())
        .log("POV -> USER Disconnected: {}", SoulFireAdventure.PLAIN_MESSAGE_SERIALIZER.serialize(event.getReason()));
    }

    private void executeSync(Session clientSession) {
      try {
        syncBotToUser(botConnection, clientSession);
      } catch (Throwable t) {
        log.error("Failed to sync bot to user", t);
        clientSession.send(new ClientboundSystemChatPacket(
          Component.text("Error while syncing you with the bot! Report this error to SoulFire developers.")
            .color(NamedTextColor.RED),
          false));
      }

      // Give the client a few moments to process the packets
      TimeUtil.waitTime(2, TimeUnit.SECONDS);

      if (lastClientSettings != null && botConnection.settingsSource().get(POVServerSettings.USE_USER_SETTINGS)) {
        botConnection.sendPacket(lastClientSettings);
      }

      lastPosition = botConnection.dataManager().localPlayer().pos();
      enableForwarding = true;

      clientSession.send(
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
    }

    @RequiredArgsConstructor
    private class B2SAdapter extends SessionAdapter {
      private final Session clientSession;

      @Override
      public void packetReceived(Session botSession, Packet packet) {
        log.trace("SERVER -> BOT: {}", packet.getClass().getSimpleName());
        if (!enableForwarding) {
          return;
        }

        botConnection.preTickHooks().add(() -> {
          switch (packet) {
            case ClientboundKeepAlivePacket ignored -> {
            }
            case ClientboundPingPacket ignored -> {
            }
            case ClientboundCustomPayloadPacket ignored -> {
            }
            case ClientboundPlayerChatPacket chatPacket -> // To avoid signature issues since the signature is for the bot, not the connected user
              clientSession.send(new ClientboundSystemChatPacket(
                botConnection.dataManager().prepareChatTypeMessage(
                  chatPacket.getChatType(),
                  new SFChatType.BoundChatMessageInfo(
                    botConnection.dataManager().getComponentForPlayerChat(chatPacket),
                    chatPacket.getName(),
                    chatPacket.getTargetName()
                  )),
                false
              ));
            default -> // MC Server of the bot -> MC Client
              clientSession.send(packet);
          }
        });
      }

      @Override
      public void packetSent(Session botSession, Packet packet) {
        log.trace("BOT -> SERVER: {}", packet.getClass().getSimpleName());
        if (!enableForwarding) {
          return;
        }

        var clientEntity = botConnection.dataManager().localPlayer();
        // Bot -> MC Client
        botConnection.preTickHooks().add(() -> {
          switch (packet) {
            case ServerboundMovePlayerPosRotPacket ignored -> {
              if (!clientEntity.pos().equals(lastPosition)) {
                clientSession.send(
                  new ClientboundPlayerPositionPacket(
                    Integer.MIN_VALUE,
                    clientEntity.pos(),
                    clientEntity.deltaMovement(),
                    clientEntity.yRot(),
                    clientEntity.xRot(),
                    List.of()
                  ));
              }
            }
            case ServerboundMovePlayerPosPacket ignored -> {
              if (!clientEntity.pos().equals(lastPosition)) {
                clientSession.send(
                  new ClientboundPlayerPositionPacket(
                    Integer.MIN_VALUE,
                    clientEntity.pos(),
                    clientEntity.deltaMovement(),
                    0,
                    0,
                    List.of(PositionElement.Y_ROT, PositionElement.X_ROT)
                  ));
              }
            }
            case ServerboundMovePlayerRotPacket ignored -> {
              if (!clientEntity.pos().equals(lastPosition)) {
                clientSession.send(
                  new ClientboundPlayerPositionPacket(
                    Integer.MIN_VALUE,
                    Vector3d.ZERO,
                    Vector3d.ZERO,
                    clientEntity.yRot(),
                    clientEntity.xRot(),
                    List.of(PositionElement.X, PositionElement.Y, PositionElement.Z, PositionElement.DELTA_X, PositionElement.DELTA_Y, PositionElement.DELTA_Z)
                  ));
              }
            }
            default -> {
              // We don't need to handle all Bot -> Server packets
            }
          }
        });
      }
    }
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class POVServerSettings implements SettingsObject {
    private static final String NAMESPACE = "pov-server";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable POV server")
        .description("Host a POV server for the bots")
        .defaultValue(false)
        .build();
    public static final IntProperty PORT_START =
      ImmutableIntProperty.builder()
        .namespace(NAMESPACE)
        .key("port-start")
        .uiName("Port Start")
        .description("What port to start with to host the POV server")
        .defaultValue(31765)
        .minValue(1)
        .maxValue(65535)
        .thousandSeparator(false)
        .build();
    public static final BooleanProperty USE_USER_SETTINGS =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("use-user-settings")
        .uiName("Use user settings")
        .description("Whether the Minecraft client of the POV User should be applied on the bot. Otherwise the user will use the bot's settings.")
        .defaultValue(true)
        .build();
  }
}
