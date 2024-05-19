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
package com.soulfiremc.server.protocol.bot;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.event.bot.BotJoinedEvent;
import com.soulfiremc.server.api.event.bot.BotPostEntityTickEvent;
import com.soulfiremc.server.api.event.bot.BotPreEntityTickEvent;
import com.soulfiremc.server.api.event.bot.ChatMessageReceiveEvent;
import com.soulfiremc.server.data.Attribute;
import com.soulfiremc.server.data.AttributeType;
import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.data.ModifierOperation;
import com.soulfiremc.server.data.Registry;
import com.soulfiremc.server.data.RegistryKeys;
import com.soulfiremc.server.data.ResourceKey;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.BuiltInKnownPackRegistry;
import com.soulfiremc.server.protocol.SFProtocolConstants;
import com.soulfiremc.server.protocol.SFProtocolHelper;
import com.soulfiremc.server.protocol.bot.container.InventoryManager;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.container.WindowContainer;
import com.soulfiremc.server.protocol.bot.model.AbilitiesData;
import com.soulfiremc.server.protocol.bot.model.ChunkKey;
import com.soulfiremc.server.protocol.bot.model.DefaultSpawnData;
import com.soulfiremc.server.protocol.bot.model.DifficultyData;
import com.soulfiremc.server.protocol.bot.model.ExperienceData;
import com.soulfiremc.server.protocol.bot.model.HealthData;
import com.soulfiremc.server.protocol.bot.model.LoginPacketData;
import com.soulfiremc.server.protocol.bot.model.ServerPlayData;
import com.soulfiremc.server.protocol.bot.movement.ControlState;
import com.soulfiremc.server.protocol.bot.state.Biome;
import com.soulfiremc.server.protocol.bot.state.BorderState;
import com.soulfiremc.server.protocol.bot.state.DimensionType;
import com.soulfiremc.server.protocol.bot.state.EntityTrackerState;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.protocol.bot.state.MapDataState;
import com.soulfiremc.server.protocol.bot.state.PlayerListState;
import com.soulfiremc.server.protocol.bot.state.TagsState;
import com.soulfiremc.server.protocol.bot.state.TickHookContext;
import com.soulfiremc.server.protocol.bot.state.WeatherState;
import com.soulfiremc.server.protocol.bot.state.entity.ClientEntity;
import com.soulfiremc.server.protocol.bot.state.entity.ExperienceOrbEntity;
import com.soulfiremc.server.protocol.bot.state.entity.RawEntity;
import com.soulfiremc.server.settings.lib.SettingsHolder;
import com.soulfiremc.server.util.PrimitiveHelper;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.ToString;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.lenni0451.lambdaevents.EventHandler;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.data.UnexpectedEncryptionException;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.data.game.KnownPack;
import org.geysermc.mcprotocollib.protocol.data.game.ResourcePackStatus;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.PaletteType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.GlobalPos;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.LimitedCraftingValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.RainStrengthValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.RespawnScreenValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.ThunderStrengthValue;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundDisconnectPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPushPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundResourcePackPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundSelectKnownPacks;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundChangeDifficultyPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCooldownPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundServerDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveMobEffectPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRotateHeadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityMotionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundUpdateAttributesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundUpdateMobEffectPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundBlockChangedAckPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerLookAtPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetCarriedItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetExperiencePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddExperienceOrbPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundHorseScreenOpenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenBookPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundChunksBiomesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundMapItemDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetChunkCacheCenterPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetChunkCacheRadiusPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetSimulationDistancePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundSetBorderCenterPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundSetBorderLerpSizePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundSetBorderSizePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundSetBorderWarningDelayPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundSetBorderWarningDistancePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Getter
@ToString
public final class SessionDataManager {
  private final SettingsHolder settingsHolder;
  private final Logger log;
  private final MinecraftCodecHelper codecHelper;
  private final BotConnection connection;
  private final WeatherState weatherState = new WeatherState();
  private final PlayerListState playerListState = new PlayerListState();
  private final Int2IntMap itemCoolDowns = Int2IntMaps.synchronize(new Int2IntOpenHashMap());
  private final Registry<DimensionType> dimensions = new Registry<>(RegistryKeys.DIMENSION_TYPE);
  private final Registry<Biome> biomes = new Registry<>(RegistryKeys.BIOME);
  private final Int2ObjectMap<MapDataState> mapDataStates = new Int2ObjectOpenHashMap<>();
  private final EntityTrackerState entityTrackerState = new EntityTrackerState();
  private final InventoryManager inventoryManager;
  private final BotActionManager botActionManager;
  private final ControlState controlState = new ControlState();
  private final TagsState tagsState = new TagsState();
  private List<KnownPack> serverKnownPacks;
  private ClientEntity clientEntity;
  private @Nullable ServerPlayData serverPlayData;
  private BorderState borderState;
  private HealthData healthData;
  private GameMode gameMode = null;
  private @Nullable GameMode previousGameMode = null;
  private GameProfile botProfile;
  private LoginPacketData loginData;
  private boolean enableRespawnScreen;
  private boolean doLimitedCrafting;
  private Level lastSpawnedInLevel;
  private int serverViewDistance = -1;
  private int serverSimulationDistance = -1;
  private @Nullable GlobalPos lastDeathPos;
  private int portalCooldown = -1;
  private @Nullable DifficultyData difficultyData;
  private @Nullable AbilitiesData abilitiesData;
  private @Nullable DefaultSpawnData defaultSpawnData;
  private @Nullable ExperienceData experienceData;
  private @Nullable ChunkKey centerChunk;
  private boolean isDead = false;
  private boolean joinedWorld = false;
  private String serverBrand;

  public SessionDataManager(BotConnection connection) {
    this.settingsHolder = connection.settingsHolder();
    this.log = connection.logger();
    this.codecHelper = connection.session().getCodecHelper();
    this.connection = connection;
    this.inventoryManager = new InventoryManager(this, connection);
    this.botActionManager = new BotActionManager(this, connection);
  }

  private static String toPlainText(Component component) {
    return SoulFireServer.PLAIN_MESSAGE_SERIALIZER.serialize(component);
  }

  private static List<String> readChannels(ClientboundCustomPayloadPacket packet) {
    var split = PrimitiveHelper.split(packet.getData(), (byte) 0x00);
    var list = new ArrayList<String>();
    for (var channel : split) {
      if (channel.length == 0) {
        continue;
      }

      list.add(new String(channel));
    }

    return list;
  }

  @EventHandler
  public void onLoginSuccess(ClientboundGameProfilePacket packet) {
    botProfile = packet.getProfile();
  }

  @EventHandler
  public void onKnownPacks(ClientboundSelectKnownPacks packet) {
    serverKnownPacks = packet.getKnownPacks();
  }

  @EventHandler
  public void onRegistry(ClientboundRegistryDataPacket packet) {
    @Subst("empty") var registry = packet.getRegistry();
    var registryKey = ResourceKey.key(registry);
    Registry.RegistryDataWriter registryWriter;
    if (registryKey.equals(RegistryKeys.DIMENSION_TYPE)) {
      registryWriter = dimensions.writer(DimensionType::new);
    } else if (registryKey.equals(RegistryKeys.BIOME)) {
      registryWriter = biomes.writer(Biome::new);
    } else {
      log.debug("Received registry data for unknown registry {}", registryKey);
      return;
    }

    var entries = packet.getEntries();
    for (var i = 0; i < entries.size(); i++) {
      var entry = entries.get(i);
      @Subst("empty") var key = entry.getId();
      var holderKey = Key.key(key);
      var providedData = entry.getData();
      NbtMap usedData;
      if (providedData == null) {
        usedData = BuiltInKnownPackRegistry.INSTANCE.mustFindData(registryKey, holderKey, serverKnownPacks);
      } else {
        usedData = providedData;
      }

      registryWriter.register(holderKey, i, usedData);
    }
  }

  @EventHandler
  public void onJoin(ClientboundLoginPacket packet) {
    // Set data from the packet
    loginData =
      new LoginPacketData(packet.isHardcore(), packet.getWorldNames(), packet.getMaxPlayers(), packet.isEnforcesSecureChat());

    enableRespawnScreen = packet.isEnableRespawnScreen();
    doLimitedCrafting = packet.isDoLimitedCrafting();
    serverViewDistance = packet.getViewDistance();
    serverSimulationDistance = packet.getSimulationDistance();

    processSpawnInfo(packet.getCommonPlayerSpawnInfo());

    // Init client entity
    clientEntity =
      new ClientEntity(packet.getEntityId(), botProfile.getId(), connection, this, controlState, currentLevel());
    clientEntity.showReducedDebug(packet.isReducedDebugInfo());
    entityTrackerState.addEntity(clientEntity);
  }

  private void processSpawnInfo(PlayerSpawnInfo spawnInfo) {
    lastSpawnedInLevel =
      new Level(
        tagsState,
        dimensions.getById(spawnInfo.getDimension()),
        Key.key(spawnInfo.getWorldName()),
        spawnInfo.getHashedSeed(),
        spawnInfo.isDebug(),
        spawnInfo.isFlat());
    gameMode = spawnInfo.getGameMode();
    previousGameMode = spawnInfo.getPreviousGamemode();
    lastDeathPos = spawnInfo.getLastDeathPos();
    portalCooldown = spawnInfo.getPortalCooldown();
  }

  @EventHandler
  public void onPosition(ClientboundPlayerPositionPacket packet) {
    var relative = packet.getRelative();
    var x = relative.contains(PositionElement.X) ? clientEntity.x() + packet.getX() : packet.getX();
    var y = relative.contains(PositionElement.Y) ? clientEntity.y() + packet.getY() : packet.getY();
    var z = relative.contains(PositionElement.Z) ? clientEntity.z() + packet.getZ() : packet.getZ();
    var yaw =
      relative.contains(PositionElement.YAW)
        ? clientEntity.yaw() + packet.getYaw()
        : packet.getYaw();
    var pitch =
      relative.contains(PositionElement.PITCH)
        ? clientEntity.pitch() + packet.getPitch()
        : packet.getPitch();

    clientEntity.setPosition(x, y, z);
    clientEntity.setRotation(yaw, pitch);

    var position = clientEntity.blockPos();
    if (!joinedWorld) {
      joinedWorld = true;

      log.info(
        "Joined server at position: X {} Y {} Z {}",
        position.getX(),
        position.getY(),
        position.getZ());

      connection.eventBus().call(new BotJoinedEvent(connection));
    } else {
      log.debug(
        "Position updated: X {} Y {} Z {}", position.getX(), position.getY(), position.getZ());
    }

    connection.sendPacket(new ServerboundAcceptTeleportationPacket(packet.getTeleportId()));
    connection.sendPacket(new ServerboundMovePlayerPosRotPacket(false, x, y, z, yaw, pitch));
  }

  @EventHandler
  public void onLookAt(ClientboundPlayerLookAtPacket packet) {
    var targetPosition = Vector3d.from(packet.getX(), packet.getY(), packet.getZ());
    if (packet.getTargetEntityOrigin() != null) {
      var entity = entityTrackerState.getEntity(packet.getTargetEntityId());
      if (entity != null) {
        targetPosition = entity.originPosition(packet.getTargetEntityOrigin());
      }
    }

    clientEntity.lookAt(packet.getOrigin(), targetPosition);
  }

  @EventHandler
  public void onRespawn(ClientboundRespawnPacket packet) {
    processSpawnInfo(packet.getCommonPlayerSpawnInfo());

    // We are now possibly in a new dimension
    clientEntity.level(currentLevel());

    log.info("Respawned");
  }

  @EventHandler
  public void onDeath(ClientboundPlayerCombatKillPacket packet) {
    var state = entityTrackerState.getEntity(packet.getPlayerId());

    if (state == null || state != clientEntity) {
      log.warn("Received death for unknown or invalid entity {}", packet.getPlayerId());
      return;
    }

    if (enableRespawnScreen) {
      log.info("Died");
      isDead = true;
    } else {
      log.info("Died, respawning due to game rule");
      connection.sendPacket(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
    }
  }

  @EventHandler
  public void onServerPlayData(ClientboundServerDataPacket packet) {
    serverPlayData =
      new ServerPlayData(packet.getMotd(), packet.getIconBytes());
  }

  @EventHandler
  public void onPluginMessage(ClientboundCustomPayloadPacket packet) {
    var channelKey = Key.key(packet.getChannel());
    log.debug("Received plugin message on channel {}", channelKey);
    if (channelKey.equals(SFProtocolConstants.BRAND_PAYLOAD_KEY)) {
      serverBrand = codecHelper.readString(Unpooled.wrappedBuffer(packet.getData()));
      log.debug("Received server brand \"{}\"", serverBrand);
    } else if (channelKey.equals(SFProtocolConstants.REGISTER_KEY)) {
      log.debug(
        "Received register packet for channels: {}", String.join(", ", readChannels(packet)));
    } else if (channelKey.equals(SFProtocolConstants.UNREGISTER_KEY)) {
      log.debug(
        "Received unregister packet for channels; {}",
        String.join(", ", readChannels(packet)));
    }
  }

  @EventHandler
  public void onPlayerChat(ClientboundPlayerChatPacket packet) {
    var message = packet.getUnsignedContent();
    if (message != null) {
      onChat(packet.getTimeStamp(), message);
      return;
    }

    var sender = ChatMessageReceiveEvent.ChatMessageSender.fromClientboundPlayerChatPacket(packet);

    onChat(packet.getTimeStamp(), Component.text(packet.getContent()), sender);
  }

  @EventHandler
  public void onServerChat(ClientboundSystemChatPacket packet) {
    onChat(System.currentTimeMillis(), packet.getContent());
  }

  @EventHandler
  public void onDisguisedChat(ClientboundDisguisedChatPacket packet) {}

  private void onChat(long stamp, Component message) {
    connection.eventBus().call(new ChatMessageReceiveEvent(connection, stamp, message, null));
  }

  private void onChat(
    long stamp, Component message, ChatMessageReceiveEvent.ChatMessageSender sender) {
    connection.eventBus().call(new ChatMessageReceiveEvent(connection, stamp, message, sender));
  }

  @EventHandler
  public void onPlayerListHeaderFooter(ClientboundTabListPacket packet) {
    playerListState.header(packet.getHeader());
    playerListState.footer(packet.getFooter());
  }

  @EventHandler
  public void onPlayerListUpdate(ClientboundPlayerInfoUpdatePacket packet) {
    for (var update : packet.getEntries()) {
      var entry = playerListState.entries().computeIfAbsent(update.getProfileId(), k -> update);
      for (var action : packet.getActions()) {
        switch (action) {
          case ADD_PLAYER -> entry.setProfile(update.getProfile());
          case INITIALIZE_CHAT -> {
            entry.setSessionId(update.getSessionId());
            entry.setExpiresAt(update.getExpiresAt());
            entry.setKeySignature(update.getKeySignature());
            entry.setPublicKey(update.getPublicKey());
          }
          case UPDATE_GAME_MODE -> entry.setGameMode(update.getGameMode());
          case UPDATE_LISTED -> entry.setListed(update.isListed());
          case UPDATE_LATENCY -> entry.setLatency(update.getLatency());
          case UPDATE_DISPLAY_NAME -> entry.setDisplayName(update.getDisplayName());
        }
      }
    }
  }

  @EventHandler
  public void onPlayerListRemove(ClientboundPlayerInfoRemovePacket packet) {
    for (var profileId : packet.getProfileIds()) {
      playerListState.entries().remove(profileId);
    }
  }

  @EventHandler
  public void onSetSimulationDistance(ClientboundSetSimulationDistancePacket packet) {
    serverSimulationDistance = packet.getSimulationDistance();
  }

  @EventHandler
  public void onSetViewDistance(ClientboundSetChunkCacheRadiusPacket packet) {
    serverViewDistance = packet.getViewDistance();
  }

  @EventHandler
  public void onSetDifficulty(ClientboundChangeDifficultyPacket packet) {
    difficultyData = new DifficultyData(packet.getDifficulty(), packet.isDifficultyLocked());
  }

  @EventHandler
  public void onAbilities(ClientboundPlayerAbilitiesPacket packet) {
    abilitiesData =
      new AbilitiesData(
        packet.isInvincible(),
        packet.isFlying(),
        packet.isCanFly(),
        packet.isCreative(),
        packet.getFlySpeed(),
        packet.getWalkSpeed());

    var attributeState = clientEntity.attributeState();
    attributeState
      .getOrCreateAttribute(AttributeType.GENERIC_MOVEMENT_SPEED)
      .baseValue(abilitiesData.walkSpeed());
    attributeState
      .getOrCreateAttribute(AttributeType.GENERIC_FLYING_SPEED)
      .baseValue(abilitiesData.flySpeed());

    controlState.flying(abilitiesData.flying());
  }

  @EventHandler
  public void onUpdateTags(ClientboundUpdateTagsPacket packet) {
    tagsState.handleTagData(packet.getTags());
  }

  @EventHandler
  public void onCompassTarget(ClientboundSetDefaultSpawnPositionPacket packet) {
    defaultSpawnData = new DefaultSpawnData(packet.getPosition(), packet.getAngle());
  }

  @EventHandler
  public void onHealth(ClientboundSetHealthPacket packet) {
    this.healthData = new HealthData(packet.getHealth(), packet.getFood(), packet.getSaturation());

    if (healthData.health() < 1) {
      this.isDead = true;
    }

    log.debug("Health updated: {}", healthData);
  }

  @EventHandler
  public void onExperience(ClientboundSetExperiencePacket packet) {
    experienceData =
      new ExperienceData(packet.getExperience(), packet.getLevel(), packet.getTotalExperience());
  }

  @EventHandler
  public void onLevelTime(ClientboundSetTimePacket packet) {
    var level = currentLevel();

    level.worldAge(packet.getWorldAge());
    level.time(packet.getTime());
  }

  @EventHandler
  public void onSetContainerContent(ClientboundContainerSetContentPacket packet) {
    inventoryManager.lastStateId(packet.getStateId());
    var container = inventoryManager.getContainer(packet.getContainerId());

    if (container == null) {
      log.warn(
        "Received container content update for unknown container {}", packet.getContainerId());
      return;
    }

    for (var i = 0; i < packet.getItems().length; i++) {
      container.setSlot(i, SFItemStack.from(packet.getItems()[i]));
    }
  }

  @EventHandler
  public void onSetContainerSlot(ClientboundContainerSetSlotPacket packet) {
    inventoryManager.lastStateId(packet.getStateId());
    if (packet.getContainerId() == -1 && packet.getSlot() == -1) {
      inventoryManager.cursorItem(SFItemStack.from(packet.getItem()));
      return;
    }

    var container = inventoryManager.getContainer(packet.getContainerId());

    if (container == null) {
      log.warn("Received container slot update for unknown container {}", packet.getContainerId());
      return;
    }

    container.setSlot(packet.getSlot(), SFItemStack.from(packet.getItem()));
  }

  @EventHandler
  public void onSetContainerData(ClientboundContainerSetDataPacket packet) {
    var container = inventoryManager.getContainer(packet.getContainerId());

    if (container == null) {
      log.warn("Received container data update for unknown container {}", packet.getContainerId());
      return;
    }

    container.setProperty(packet.getRawProperty(), packet.getValue());
  }

  @EventHandler
  public void onSetSlot(ClientboundSetCarriedItemPacket packet) {
    inventoryManager.heldItemSlot(packet.getSlot());
  }

  @EventHandler
  public void onOpenScreen(ClientboundOpenScreenPacket packet) {
    var container =
      new WindowContainer(packet.getType(), packet.getTitle(), packet.getContainerId());
    inventoryManager.setContainer(packet.getContainerId(), container);
    inventoryManager.openContainer(container);
  }

  @EventHandler
  public void onOpenBookScreen(ClientboundOpenBookPacket packet) {}

  @EventHandler
  public void onOpenHorseScreen(ClientboundHorseScreenOpenPacket packet) {}

  @EventHandler
  public void onCloseContainer(ClientboundContainerClosePacket packet) {
    inventoryManager.openContainer(null);
  }

  @EventHandler
  public void onMapData(ClientboundMapItemDataPacket packet) {
    mapDataStates.computeIfAbsent(packet.getMapId(), k -> new MapDataState(packet)).update(packet);
  }

  @EventHandler
  public void onCooldown(ClientboundCooldownPacket packet) {
    if (packet.getCooldownTicks() == 0) {
      itemCoolDowns.remove(packet.getItemId());
    } else {
      itemCoolDowns.put(packet.getItemId(), packet.getCooldownTicks());
    }
  }

  @EventHandler
  public void onGameEvent(ClientboundGameEventPacket packet) {
    switch (packet.getNotification()) {
      case INVALID_BED -> log.info("Bot had no bed/respawn anchor to respawn at (was maybe obstructed)");
      case START_RAIN -> weatherState.raining(true);
      case STOP_RAIN -> weatherState.raining(false);
      case CHANGE_GAMEMODE -> {
        previousGameMode = gameMode;
        gameMode = (GameMode) packet.getValue();
      }
      case ENTER_CREDITS -> {
        log.info("Entered credits {} (Respawning now)", packet.getValue());
        connection.sendPacket(
          new ServerboundClientCommandPacket(ClientCommand.RESPAWN)); // Respawns the player
      }
      case DEMO_MESSAGE -> log.debug("Demo event: {}", packet.getValue());
      case ARROW_HIT_PLAYER -> log.debug("Arrow hit player");
      case RAIN_STRENGTH -> weatherState.rainStrength(((RainStrengthValue) packet.getValue()).getStrength());
      case THUNDER_STRENGTH -> weatherState.thunderStrength(((ThunderStrengthValue) packet.getValue()).getStrength());
      case PUFFERFISH_STING_SOUND -> log.debug("Pufferfish sting sound");
      case AFFECTED_BY_ELDER_GUARDIAN -> log.debug("Affected by elder guardian");
      case ENABLE_RESPAWN_SCREEN -> enableRespawnScreen = packet.getValue() == RespawnScreenValue.ENABLE_RESPAWN_SCREEN;
      case LIMITED_CRAFTING -> doLimitedCrafting = packet.getValue() == LimitedCraftingValue.LIMITED_CRAFTING;
    }
  }

  @EventHandler
  public void onSetCenterChunk(ClientboundSetChunkCacheCenterPacket packet) {
    centerChunk = new ChunkKey(packet.getChunkX(), packet.getChunkZ());
  }

  @EventHandler
  public void onChunkData(ClientboundLevelChunkWithLightPacket packet) {
    var level = currentLevel();

    var data = packet.getChunkData();
    var buf = Unpooled.wrappedBuffer(data);

    var chunkData = level.chunks().getOrCreateChunk(packet.getX(), packet.getZ());

    try {
      for (var i = 0; i < chunkData.getSectionCount(); i++) {
        chunkData.setSection(i, SFProtocolHelper.readChunkSection(buf, codecHelper));
      }
    } catch (IOException e) {
      log.error("Failed to read chunk section", e);
    }
  }

  @EventHandler
  public void onChunkData(ClientboundChunksBiomesPacket packet) {
    var level = currentLevel();

    for (var biomeData : packet.getChunkBiomeData()) {
      var chunkData = level.chunks().getChunk(biomeData.getX(), biomeData.getZ());

      // Vanilla silently ignores updates for unknown chunks
      if (chunkData == null) {
        return;
      }

      var buf = Unpooled.wrappedBuffer(biomeData.getBuffer());
      for (var i = 0; chunkData.getSectionCount() > i; i++) {
        var section = chunkData.getSection(i);
        var biomePalette = codecHelper.readDataPalette(buf, PaletteType.BIOME);
        chunkData.setSection(
          i, new ChunkSection(section.getBlockCount(), section.getChunkData(), biomePalette));
      }
    }
  }

  @EventHandler
  public void onChunkForget(ClientboundForgetLevelChunkPacket packet) {
    var level = currentLevel();
    level.chunks().removeChunk(packet.getX(), packet.getZ());
  }

  @EventHandler
  public void onSectionBlockUpdate(ClientboundSectionBlocksUpdatePacket packet) {
    var level = currentLevel();
    var chunkData = level.chunks().getChunk(packet.getChunkX(), packet.getChunkZ());

    // Vanilla silently ignores updates for unknown chunks
    if (chunkData == null) {
      return;
    }

    for (var entry : packet.getEntries()) {
      var vector3i = entry.getPosition();
      var newId = entry.getBlock();

      log.debug("Updating block at {} to {}", vector3i, newId);
      level.setBlockId(vector3i, newId);
    }
  }

  @EventHandler
  public void onBlockUpdate(ClientboundBlockUpdatePacket packet) {
    var level = currentLevel();
    var entry = packet.getEntry();

    var vector3i = entry.getPosition();
    var newId = entry.getBlock();

    level.setBlockId(vector3i, newId);

    log.debug("Updated block at {} to {}", vector3i, newId);
  }

  @EventHandler
  public void onBlockChangedAck(ClientboundBlockChangedAckPacket packet) {
    // TODO: Implement block break
  }

  @EventHandler
  public void onBorderInit(ClientboundInitializeBorderPacket packet) {
    borderState =
      new BorderState(
        packet.getNewCenterX(),
        packet.getNewCenterZ(),
        packet.getOldSize(),
        packet.getNewSize(),
        packet.getLerpTime(),
        packet.getNewAbsoluteMaxSize(),
        packet.getWarningBlocks(),
        packet.getWarningTime());
  }

  @EventHandler
  public void onBorderCenter(ClientboundSetBorderCenterPacket packet) {
    borderState.centerX(packet.getNewCenterX());
    borderState.centerZ(packet.getNewCenterZ());
  }

  @EventHandler
  public void onBorderLerpSize(ClientboundSetBorderLerpSizePacket packet) {
    borderState.oldSize(packet.getOldSize());
    borderState.newSize(packet.getNewSize());
    borderState.lerpTime(packet.getLerpTime());
  }

  @EventHandler
  public void onBorderSize(ClientboundSetBorderSizePacket packet) {
    borderState.oldSize(borderState.newSize());
    borderState.newSize(packet.getSize());
  }

  @EventHandler
  public void onBorderWarningTime(ClientboundSetBorderWarningDelayPacket packet) {
    borderState.warningTime(packet.getWarningDelay());
  }

  @EventHandler
  public void onBorderWarningBlocks(ClientboundSetBorderWarningDistancePacket packet) {
    borderState.warningBlocks(packet.getWarningBlocks());
  }

  @EventHandler
  public void onEntitySpawn(ClientboundAddEntityPacket packet) {
    var entityState =
      new RawEntity(
        packet.getEntityId(),
        packet.getUuid(),
        EntityType.REGISTRY.getById(packet.getType().ordinal()),
        packet.getData(),
        currentLevel(),
        packet.getX(),
        packet.getY(),
        packet.getZ(),
        packet.getYaw(),
        packet.getPitch(),
        packet.getHeadYaw(),
        packet.getMotionX(),
        packet.getMotionY(),
        packet.getMotionZ());

    entityTrackerState.addEntity(entityState);
  }

  @EventHandler
  public void onExperienceOrbSpawn(ClientboundAddExperienceOrbPacket packet) {
    var experienceOrbState =
      new ExperienceOrbEntity(packet.getEntityId(), packet.getExp(), currentLevel(), packet.getX(), packet.getY(),
        packet.getZ());

    entityTrackerState.addEntity(experienceOrbState);
  }

  @EventHandler
  public void onEntityRemove(ClientboundRemoveEntitiesPacket packet) {
    for (var entityId : packet.getEntityIds()) {
      entityTrackerState.removeEntity(entityId);
    }
  }

  @EventHandler
  public void onEntityMetadata(ClientboundSetEntityDataPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.warn("Received entity metadata packet for unknown entity {}", packet.getEntityId());
      return;
    }

    for (var entry : packet.getMetadata()) {
      state.metadataState().setMetadata(entry);
    }
  }

  @EventHandler
  public void onEntityAttributes(ClientboundUpdateAttributesPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.warn("Received entity attributes packet for unknown entity {}", packet.getEntityId());
      return;
    }

    for (var entry : packet.getAttributes()) {
      var key = Key.key(entry.getType().getIdentifier());
      var attributeType = AttributeType.REGISTRY.getByKey(key);
      if (attributeType == null) {
        log.warn("Received unknown attribute type {}", key);
        continue;
      }

      var attribute =
        state.attributeState().getOrCreateAttribute(attributeType).baseValue(entry.getValue());

      attribute.modifiers().clear();
      attribute
        .modifiers()
        .putAll(
          entry.getModifiers().stream()
            .map(
              modifier ->
                new Attribute.Modifier(
                  modifier.getUuid(),
                  modifier.getAmount(),
                  switch (modifier.getOperation()) {
                    case ADD -> ModifierOperation.ADD_VALUE;
                    case ADD_MULTIPLIED_BASE -> ModifierOperation.ADD_MULTIPLIED_BASE;
                    case ADD_MULTIPLIED_TOTAL -> ModifierOperation.ADD_MULTIPLIED_TOTAL;
                  }))
            .collect(Collectors.toMap(Attribute.Modifier::uuid, Function.identity())));
    }
  }

  @EventHandler
  public void onEntityEvent(ClientboundEntityEventPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.warn("Received entity event packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.handleEntityEvent(packet.getEvent());
  }

  @EventHandler
  public void onUpdateEffect(ClientboundUpdateMobEffectPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.warn("Received update effect packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state
      .effectState()
      .updateEffect(
        packet.getEffect(),
        packet.getAmplifier(),
        packet.getDuration(),
        packet.isAmbient(),
        packet.isShowParticles(),
        packet.isShowIcon(),
        packet.isBlend());
  }

  @EventHandler
  public void onRemoveEffect(ClientboundRemoveMobEffectPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.warn("Received remove effect packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.effectState().removeEffect(packet.getEffect());
  }

  @EventHandler
  public void onEntityMotion(ClientboundSetEntityMotionPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.warn("Received entity motion packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.setMotion(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
  }

  @EventHandler
  public void onEntityPos(ClientboundMoveEntityPosPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.warn("Received entity position packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.addPosition(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ());
    state.onGround(packet.isOnGround());
  }

  @EventHandler
  public void onEntityRot(ClientboundMoveEntityRotPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.warn("Received entity rotation packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.setRotation(packet.getYaw(), packet.getPitch());
    state.onGround(packet.isOnGround());
  }

  @EventHandler
  public void onEntityRot(ClientboundRotateHeadPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.warn("Received entity head rotation packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.setHeadRotation(packet.getHeadYaw());
  }

  @EventHandler
  public void onEntityPosRot(ClientboundMoveEntityPosRotPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.warn(
        "Received entity position rotation packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.addPosition(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ());
    state.setRotation(packet.getYaw(), packet.getPitch());
    state.onGround(packet.isOnGround());
  }

  @EventHandler
  public void onEntityTeleport(ClientboundTeleportEntityPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.warn("Received entity teleport packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.setPosition(packet.getX(), packet.getY(), packet.getZ());
    state.setRotation(packet.getYaw(), packet.getPitch());
    state.onGround(packet.isOnGround());
  }

  @EventHandler
  public void onResourcePack(ClientboundResourcePackPushPacket packet) {
    if (!isValidResourcePackUrl(packet.getUrl())) {
      connection.sendPacket(new ServerboundResourcePackPacket(packet.getId(), ResourcePackStatus.INVALID_URL));
      return;
    }

    var version = connection.protocolVersion();
    if (SFVersionConstants.isBedrock(version)) {
      connection.sendPacket(new ServerboundResourcePackPacket(packet.getId(), ResourcePackStatus.DECLINED));
      return;
    }

    connection.sendPacket(new ServerboundResourcePackPacket(packet.getId(), ResourcePackStatus.ACCEPTED));
    connection.sendPacket(new ServerboundResourcePackPacket(packet.getId(), ResourcePackStatus.DOWNLOADED));
    connection.sendPacket(
      new ServerboundResourcePackPacket(packet.getId(), ResourcePackStatus.SUCCESSFULLY_LOADED));
  }

  private boolean isValidResourcePackUrl(String url) {
    try {
      var protocol = URI.create(url).toURL().getProtocol();
      return "http".equals(protocol) || "https".equals(protocol);
    } catch (MalformedURLException var3) {
      return false;
    }
  }

  @EventHandler
  public void onLoginDisconnectPacket(ClientboundLoginDisconnectPacket packet) {
    var plainMessage = toPlainText(packet.getReason());
    log.error("Login failed with reason \"{}\"", plainMessage);

    handleTips(plainMessage);
  }

  @EventHandler
  public void onDisconnectPacket(ClientboundDisconnectPacket packet) {
    var plainMessage = toPlainText(packet.getReason());
    log.info("Disconnected with reason \"{}\"", plainMessage);

    handleTips(plainMessage);
  }

  public void onDisconnectEvent(DisconnectedEvent event) {
    var reason = toPlainText(event.getReason());
    var cause = event.getCause();
    if (cause == null) { // Packet wise disconnects have no cause
      return;
    }

    if (cause.getClass() == UnexpectedEncryptionException.class) {
      log.error("Server is online mode!");
    } else if (reason.contains("Connection refused")) {
      log.error("Server is not reachable!");
    } else {
      log.error("Disconnected: {}", reason);
    }

    log.error("Cause: {}", cause.getMessage());
  }

  private void handleTips(String message) {
    var lowerCaseMessage = message.toLowerCase(Locale.ROOT);
    if (lowerCaseMessage.contains("connection throttled")) {
      log.info("Tip: The server limits the amount of connections per second. To disable this, set 'settings.connection-throttle' to 0 in bukkit.yml of the server.");
      log.info("Tip: If you don't have access to the server, you can try increasing your join delay in the bot settings.");
    }
  }

  public @NotNull Level currentLevel() {
    return Objects.requireNonNull(lastSpawnedInLevel, "Current level is not set");
  }

  public void tick() {
    // Tick border changes
    if (borderState != null) {
      borderState.tick();
    }

    // Tick cooldowns
    tickCooldowns();

    var tickHookState = TickHookContext.INSTANCE.get();

    connection.eventBus().call(new BotPreEntityTickEvent(connection));
    tickHookState.callHooks(TickHookContext.HookType.PRE_ENTITY_TICK);

    // Tick entities
    entityTrackerState.tick();

    connection.eventBus().call(new BotPostEntityTickEvent(connection));
    tickHookState.callHooks(TickHookContext.HookType.POST_ENTITY_TICK);
  }

  private void tickCooldowns() {
    if (portalCooldown > 0) {
      portalCooldown--;
    }

    synchronized (itemCoolDowns) {
      var iterator = itemCoolDowns.int2IntEntrySet().iterator();
      while (iterator.hasNext()) {
        var entry = iterator.next();
        var ticks = entry.getIntValue() - 1;
        if (ticks <= 0) {
          iterator.remove();
        } else {
          entry.setValue(ticks);
        }
      }
    }
  }
}
