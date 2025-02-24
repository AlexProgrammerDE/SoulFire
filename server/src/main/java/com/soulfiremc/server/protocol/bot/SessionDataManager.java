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

import com.soulfiremc.server.adventure.SoulFireAdventure;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.BotJoinedEvent;
import com.soulfiremc.server.api.event.bot.ChatMessageReceiveEvent;
import com.soulfiremc.server.data.*;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.BuiltInKnownPackRegistry;
import com.soulfiremc.server.protocol.SFProtocolConstants;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.container.WindowContainer;
import com.soulfiremc.server.protocol.bot.model.ChunkKey;
import com.soulfiremc.server.protocol.bot.model.ServerPlayData;
import com.soulfiremc.server.protocol.bot.state.*;
import com.soulfiremc.server.protocol.bot.state.entity.*;
import com.soulfiremc.server.protocol.bot.state.registry.Biome;
import com.soulfiremc.server.protocol.bot.state.registry.DimensionType;
import com.soulfiremc.server.protocol.bot.state.registry.SFChatType;
import com.soulfiremc.server.settings.lib.InstanceSettingsSource;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.mcstructs.LevelLoadStatusManager;
import com.soulfiremc.server.util.structs.EntityMovement;
import com.soulfiremc.server.util.structs.TickTimer;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.lenni0451.lambdaevents.EventHandler;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.UnexpectedEncryptionException;
import org.geysermc.mcprotocollib.protocol.data.game.*;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatType;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.PaletteType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeModifier;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.LimitedCraftingValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.RainStrengthValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.RespawnScreenValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.ThunderStrengthValue;
import org.geysermc.mcprotocollib.protocol.data.game.setting.Difficulty;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundDisconnectPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPushPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundResourcePackPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundSelectKnownPacks;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundUpdateEnabledFeaturesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddExperienceOrbPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginFinishedPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@ToString
public final class SessionDataManager {
  private final InstanceSettingsSource settingsSource;
  private final Logger log;
  private final BotConnection connection;
  private final PlayerListState playerListState = new PlayerListState();
  private final Map<ResourceKey<?>, List<RegistryEntry>> resolvedRegistryData = new LinkedHashMap<>();
  private final Registry<DimensionType> dimensionTypeRegistry = new Registry<>(RegistryKeys.DIMENSION_TYPE);
  private final Registry<Biome> biomeRegistry = new Registry<>(RegistryKeys.BIOME);
  private final Registry<SFChatType> chatTypeRegistry = new Registry<>(RegistryKeys.CHAT_TYPE);
  private final Int2ObjectMap<MapDataState> mapDataStates = new Int2ObjectOpenHashMap<>();
  private final TagsState tagsState = new TagsState();
  private MultiPlayerGameMode gameModeState;
  private Key[] serverEnabledFeatures;
  private List<KnownPack> serverKnownPacks;
  private LocalPlayer localPlayer;
  private LevelLoadStatusManager levelLoadStatusManager;
  private @Nullable ServerPlayData serverPlayData;
  private GameProfile botProfile;
  @Getter(value = AccessLevel.PRIVATE)
  private Level level;
  private final TickTimer tickTimer = new TickTimer(20.0F, 0L, this::getTickTargetMillis);
  private Key[] levelNames;
  private int maxPlayers = 20;
  private int serverChunkRadius = 3;
  private int serverSimulationDistance = 3;
  private boolean serverEnforcesSecureChat;
  private @Nullable ChunkKey centerChunk;
  private boolean joinedWorld = false;
  private String serverBrand;

  public SessionDataManager(BotConnection connection) {
    this.settingsSource = connection.settingsSource();
    this.log = connection.logger();
    this.connection = connection;
  }

  private static void setValuesFromPositionPacket(EntityMovement newMovement, List<PositionElement> set, Entity entity, boolean canLerp) {
    var lv = EntityMovement.ofEntityUsingLerpTarget(entity);
    var absolutePos = EntityMovement.calculateAbsolute(lv, newMovement, set);
    var teleport = lv.pos().distanceSquared(absolutePos.pos()) > 4096.0;
    if (canLerp && !teleport) {
      entity.lerpTo(absolutePos.pos().getX(), absolutePos.pos().getY(), absolutePos.pos().getZ(), absolutePos.yRot(), absolutePos.xRot(), 3);
      entity.setDeltaMovement(absolutePos.deltaMovement());
    } else {
      entity.setPos(absolutePos.pos());
      entity.setDeltaMovement(absolutePos.deltaMovement());
      entity.setYRot(absolutePos.yRot());
      entity.setXRot(absolutePos.xRot());
      var oldPos = new EntityMovement(entity.oldPosition(), Vector3d.ZERO, entity.yRotO, entity.xRotO);
      var movedOldPos = EntityMovement.calculateAbsolute(oldPos, newMovement, set);
      entity.setOldPosAndRot(movedOldPos.pos(), movedOldPos.yRot(), movedOldPos.xRot());
    }
  }

  private static List<String> readChannels(ClientboundCustomPayloadPacket packet) {
    var split = SFHelpers.split(packet.getData(), (byte) 0x00);
    var list = new ArrayList<String>();
    for (var channel : split) {
      if (channel.length == 0) {
        continue;
      }

      list.add(new String(channel));
    }

    return list;
  }

  private float getTickTargetMillis(float defaultValue) {
    if (this.level != null) {
      var tickRateManager = this.level.tickRateManager();
      if (tickRateManager.runsNormally()) {
        return Math.max(defaultValue, tickRateManager.millisecondsPerTick());
      }
    }

    return defaultValue;
  }

  @EventHandler
  public void onLoginSuccess(ClientboundLoginFinishedPacket packet) {
    botProfile = packet.getProfile();
  }

  @EventHandler
  public void onKnownPacks(ClientboundSelectKnownPacks packet) {
    serverKnownPacks = packet.getKnownPacks();
  }

  @EventHandler
  public void onUpdateEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket packet) {
    serverEnabledFeatures = packet.getFeatures();
  }

  @EventHandler
  public void onRegistry(ClientboundRegistryDataPacket packet) {
    var registry = packet.getRegistry();
    var registryKey = ResourceKey.key(registry);

    Registry.RegistryDataWriter registryWriter;
    if (registryKey.equals(RegistryKeys.DIMENSION_TYPE)) {
      registryWriter = dimensionTypeRegistry.writer(DimensionType::new);
    } else if (registryKey.equals(RegistryKeys.BIOME)) {
      registryWriter = biomeRegistry.writer(Biome::new);
    } else if (registryKey.equals(RegistryKeys.CHAT_TYPE)) {
      registryWriter = chatTypeRegistry.writer(SFChatType::new);
    } else {
      log.debug("Received registry data for unknown registry {}", registryKey);
      registryWriter = Registry.RegistryDataWriter.NO_OP;
    }

    var resolvedEntries = new ArrayList<RegistryEntry>();
    var entries = packet.getEntries();
    for (var i = 0; i < entries.size(); i++) {
      var entry = entries.get(i);
      var holderKey = entry.getId();
      var providedData = entry.getData();
      NbtMap usedData;
      if (providedData == null) {
        usedData = BuiltInKnownPackRegistry.INSTANCE.mustFindData(registryKey, holderKey, serverKnownPacks);
      } else {
        usedData = providedData;
      }

      registryWriter.register(holderKey, i, usedData);
      resolvedEntries.add(new RegistryEntry(holderKey, usedData));
    }

    resolvedRegistryData.put(registryKey, resolvedEntries);
  }

  @EventHandler
  public void onJoin(ClientboundLoginPacket packet) {
    gameModeState = new MultiPlayerGameMode(connection, this);

    levelNames = packet.getWorldNames();
    maxPlayers = packet.getMaxPlayers();
    serverChunkRadius = packet.getViewDistance();
    serverSimulationDistance = packet.getSimulationDistance();

    var spawnInfo = packet.getCommonPlayerSpawnInfo();
    var dimensionType = dimensionTypeRegistry.getById(spawnInfo.getDimension());

    level = new Level(
      connection,
      tagsState,
      dimensionType,
      spawnInfo.getWorldName(),
      spawnInfo.getHashedSeed(),
      spawnInfo.isDebug(),
      spawnInfo.getSeaLevel(),
      new Level.LevelData(Difficulty.NORMAL, packet.isHardcore(), packet.getCommonPlayerSpawnInfo().isFlat()));

    // Init client entity
    if (localPlayer == null) {
      localPlayer = new LocalPlayer(connection, currentLevel(), botProfile);
      localPlayer.setYRot(-180.0F);
      connection.inventoryManager().setContainer(0, localPlayer.inventory());
    }

    localPlayer.resetPos();
    localPlayer.entityId(packet.getEntityId());
    level.entityTracker().addEntity(localPlayer);

    this.gameModeState.adjustPlayer(localPlayer);

    startWaitingForNewLevel(localPlayer, currentLevel());

    localPlayer.setReducedDebugInfo(packet.isReducedDebugInfo());
    localPlayer.setShowDeathScreen(packet.isEnableRespawnScreen());
    localPlayer.setDoLimitedCrafting(packet.isDoLimitedCrafting());
    localPlayer.setLastDeathLocation(Optional.ofNullable(spawnInfo.getLastDeathPos()));
    localPlayer.setPortalCooldown(spawnInfo.getPortalCooldown());

    this.gameModeState.setLocalMode(localPlayer, spawnInfo.getGameMode(), spawnInfo.getPreviousGamemode());

    serverEnforcesSecureChat = packet.isEnforcesSecureChat();
  }

  @EventHandler
  public void onRespawn(ClientboundRespawnPacket packet) {
    var spawnInfo = packet.getCommonPlayerSpawnInfo();

    // Only create a new level when we actually switch levels
    if (!spawnInfo.getWorldName().equals(level.worldKey())) {
      level = new Level(
        connection,
        tagsState,
        dimensionTypeRegistry.getById(spawnInfo.getDimension()),
        spawnInfo.getWorldName(),
        spawnInfo.getHashedSeed(),
        spawnInfo.isDebug(),
        spawnInfo.getSeaLevel(),
        new Level.LevelData(this.level.levelData().difficulty(), this.level.levelData().hardcore(), spawnInfo.isFlat()));
    }

    var oldLocalPlayer = localPlayer;
    var newLocalPlayer = packet.isKeepMetadata() ? new LocalPlayer(connection, currentLevel(), botProfile, oldLocalPlayer.isShiftKeyDown(), oldLocalPlayer.isSprinting())
      : new LocalPlayer(connection, currentLevel(), botProfile);
    connection.inventoryManager().setContainer(0, newLocalPlayer.inventory());

    this.startWaitingForNewLevel(newLocalPlayer, currentLevel());

    newLocalPlayer.entityId(oldLocalPlayer.entityId());

    this.localPlayer = newLocalPlayer;
    if (packet.isKeepMetadata()) {
      newLocalPlayer.metadataState().assignValues(oldLocalPlayer.metadataState());

      newLocalPlayer.setDeltaMovement(oldLocalPlayer.deltaMovement());
      newLocalPlayer.setYRot(oldLocalPlayer.yRot());
      newLocalPlayer.setXRot(oldLocalPlayer.xRot());
    } else {
      newLocalPlayer.resetPos();
      newLocalPlayer.setYRot(-180.0F);
    }

    if (packet.isKeepAttributeModifiers()) {
      newLocalPlayer.attributeState().assignAllValues(oldLocalPlayer.attributeState());
    } else {
      newLocalPlayer.attributeState().assignBaseValues(oldLocalPlayer.attributeState());
    }

    level.entityTracker().addEntity(newLocalPlayer);

    this.gameModeState.adjustPlayer(newLocalPlayer);

    newLocalPlayer.setReducedDebugInfo(oldLocalPlayer.isReducedDebugInfo());
    newLocalPlayer.setShowDeathScreen(oldLocalPlayer.shouldShowDeathScreen());
    newLocalPlayer.setLastDeathLocation(Optional.ofNullable(spawnInfo.getLastDeathPos()));
    newLocalPlayer.setPortalCooldown(spawnInfo.getPortalCooldown());

    this.gameModeState.setLocalMode(newLocalPlayer, spawnInfo.getGameMode(), spawnInfo.getPreviousGamemode());

    log.info("Respawned");
  }

  private void startWaitingForNewLevel(LocalPlayer player, Level level) {
    this.levelLoadStatusManager = new LevelLoadStatusManager(player, level);
  }

  @EventHandler
  public void onTickingState(ClientboundTickingStatePacket packet) {
    if (this.level != null) {
      var tickRateManager = level.tickRateManager();

      tickRateManager.setTickRate(packet.getTickRate());
      tickRateManager.setFrozen(packet.isFrozen());
    }
  }

  @EventHandler
  public void onTickingStep(ClientboundTickingStepPacket packet) {
    if (this.level != null) {
      var tickRateManager = level.tickRateManager();

      tickRateManager.setFrozenTicksToRun(packet.getTickSteps());
    }
  }

  @EventHandler
  public void onPosition(ClientboundPlayerPositionPacket packet) {
    setValuesFromPositionPacket(new EntityMovement(
      packet.getPosition(),
      packet.getDeltaMovement(),
      packet.getYRot(),
      packet.getXRot()
    ), packet.getRelatives(), localPlayer, false);

    var position = localPlayer.blockPos();
    if (!joinedWorld) {
      joinedWorld = true;

      log.info(
        "Joined server at position: X {} Y {} Z {}",
        position.getX(),
        position.getY(),
        position.getZ());

      SoulFireAPI.postEvent(new BotJoinedEvent(connection));
    } else {
      log.debug(
        "Position updated: X {} Y {} Z {}", position.getX(), position.getY(), position.getZ());
    }

    connection.sendPacket(new ServerboundAcceptTeleportationPacket(packet.getId()));
    connection.sendPacket(new ServerboundMovePlayerPosRotPacket(
      false,
      false,
      localPlayer.pos().getX(),
      localPlayer.pos().getY(),
      localPlayer.pos().getZ(),
      localPlayer.yRot(),
      localPlayer.xRot()
    ));
  }

  @EventHandler
  public void onRotation(ClientboundPlayerRotationPacket packet) {
    localPlayer.setRot(
      packet.getYRot(),
      packet.getXRot()
    );
    localPlayer.setOldRot();
    connection.sendPacket(new ServerboundMovePlayerRotPacket(
      false,
      false,
      packet.getYRot(),
      packet.getXRot()
    ));
  }

  @EventHandler
  public void onLookAt(ClientboundPlayerLookAtPacket packet) {
    var targetPosition = Vector3d.from(packet.getX(), packet.getY(), packet.getZ());
    if (packet.getTargetEntityOrigin() != null) {
      var entity = level.entityTracker().getEntity(packet.getTargetEntityId());
      if (entity != null) {
        targetPosition = entity.originPosition(packet.getTargetEntityOrigin());
      }
    }

    localPlayer.lookAt(packet.getOrigin(), targetPosition);
  }

  @EventHandler
  public void onDeath(ClientboundPlayerCombatKillPacket packet) {
    var state = level.entityTracker().getEntity(packet.getPlayerId());
    if (state == localPlayer) {
      if (localPlayer.shouldShowDeathScreen()) {
        log.info("Died");
      } else {
        log.info("Died, respawning due to game rule");
        connection.sendPacket(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
      }
    }
  }

  @EventHandler
  public void onServerPlayData(ClientboundServerDataPacket packet) {
    serverPlayData =
      new ServerPlayData(packet.getMotd(), packet.getIconBytes());
  }

  @EventHandler
  public void onPluginMessage(ClientboundCustomPayloadPacket packet) {
    var channelKey = packet.getChannel();
    log.debug("Received plugin message on channel {}", channelKey);
    if (channelKey.equals(SFProtocolConstants.BRAND_PAYLOAD_KEY)) {
      serverBrand = MinecraftTypes.readString(Unpooled.wrappedBuffer(packet.getData()));
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
    onChat(packet.getTimeStamp(), prepareChatTypeMessage(packet.getChatType(), new SFChatType.BoundChatMessageInfo(
      getComponentForPlayerChat(packet),
      packet.getName(),
      packet.getTargetName()
    )));
  }

  @EventHandler
  public void onServerChat(ClientboundSystemChatPacket packet) {
    if (packet.isOverlay()) {
      // Action bar message
      return;
    }

    onChat(System.currentTimeMillis(), packet.getContent());
  }

  @EventHandler
  public void onDisguisedChat(ClientboundDisguisedChatPacket packet) {
    onChat(System.currentTimeMillis(), prepareChatTypeMessage(packet.getChatType(), new SFChatType.BoundChatMessageInfo(
      packet.getMessage(),
      packet.getName(),
      packet.getTargetName()
    )));
  }

  public Component getComponentForPlayerChat(ClientboundPlayerChatPacket packet) {
    return Objects.requireNonNullElseGet(packet.getUnsignedContent(), () -> Component.text(packet.getContent()));
  }

  public Component prepareChatTypeMessage(Holder<ChatType> chatTypeHolder, SFChatType.BoundChatMessageInfo chatInfo) {
    return SFChatType.buildChatComponent(chatTypeHolder.getOrCompute(id -> chatTypeRegistry.getById(id).mcplChatType()), chatInfo);
  }

  private void onChat(long stamp, Component message) {
    SoulFireAPI.postEvent(new ChatMessageReceiveEvent(connection, stamp, message));
  }

  @EventHandler
  public void onPlayerListHeaderFooter(ClientboundTabListPacket packet) {
    playerListState.header(packet.getHeader());
    playerListState.footer(packet.getFooter());
  }

  @EventHandler
  public void onPlayerListUpdate(ClientboundPlayerInfoUpdatePacket packet) {
    if (packet.getActions().contains(PlayerListEntryAction.ADD_PLAYER)) {
      for (var newEntry : packet.getEntries()) {
        playerListState.entries().putIfAbsent(newEntry.getProfileId(), newEntry);
      }
    }

    for (var update : packet.getEntries()) {
      var entry = playerListState.entries().get(update.getProfileId());
      if (entry == null) {
        continue;
      }

      for (var action : packet.getActions()) {
        SFHelpers.mustSupply(() -> switch (action) {
          case ADD_PLAYER -> () -> {
            // Don't handle, just like vanilla
          };
          case INITIALIZE_CHAT -> () -> {
            entry.setSessionId(update.getSessionId());
            entry.setExpiresAt(update.getExpiresAt());
            entry.setKeySignature(update.getKeySignature());
            entry.setPublicKey(update.getPublicKey());
          };
          case UPDATE_GAME_MODE -> () -> {
            if (entry.getGameMode() != update.getGameMode() && localPlayer != null && update.getProfileId().equals(localPlayer.uuid())) {
              localPlayer.onGameModeChanged(update.getGameMode());
            }

            entry.setGameMode(update.getGameMode());
          };
          case UPDATE_LISTED -> () -> entry.setListed(update.isListed());
          case UPDATE_LATENCY -> () -> entry.setLatency(update.getLatency());
          case UPDATE_DISPLAY_NAME -> () -> entry.setDisplayName(update.getDisplayName());
          case UPDATE_HAT -> () -> entry.setShowHat(update.isShowHat());
          case UPDATE_LIST_ORDER -> () -> entry.setListOrder(update.getListOrder());
        });
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
    serverChunkRadius = packet.getViewDistance();
  }

  @EventHandler
  public void onSetDifficulty(ClientboundChangeDifficultyPacket packet) {
    level.levelData().difficulty(packet.getDifficulty());
    level.levelData().difficultyLocked(packet.isDifficultyLocked());
  }

  @EventHandler
  public void onAbilities(ClientboundPlayerAbilitiesPacket packet) {
    var abilitiesData = localPlayer.abilitiesState();
    abilitiesData.flying = packet.isFlying();
    abilitiesData.instabuild = packet.isCreative();
    abilitiesData.invulnerable = packet.isInvincible();
    abilitiesData.mayfly = packet.isCanFly();
    abilitiesData.flySpeed(packet.getFlySpeed());
    abilitiesData.walkSpeed(packet.getWalkSpeed());
  }

  @EventHandler
  public void onUpdateTags(ClientboundUpdateTagsPacket packet) {
    tagsState.handleTagData(packet.getTags());
  }

  @EventHandler
  public void onCompassTarget(ClientboundSetDefaultSpawnPositionPacket packet) {
    level.levelData().setSpawn(packet.getPosition(), packet.getAngle());
  }

  @EventHandler
  public void onHealth(ClientboundSetHealthPacket packet) {
    localPlayer.hurtTo(packet.getHealth());
    localPlayer.getFoodData().setFoodLevel(packet.getFood());
    localPlayer.getFoodData().setSaturation(packet.getSaturation());
  }

  @EventHandler
  public void onExperience(ClientboundSetExperiencePacket packet) {
    localPlayer.setExperienceValues(packet.getExperience(), packet.getTotalExperience(), packet.getLevel());
  }

  @EventHandler
  public void onLevelTime(ClientboundSetTimePacket packet) {
    var level = currentLevel();

    level.setTimeFromServer(packet.getGameTime(), packet.getDayTime(), packet.isTickDayTime());
  }

  @EventHandler
  public void onSetContainerContent(ClientboundContainerSetContentPacket packet) {
    connection.inventoryManager().lastStateId(packet.getStateId());
    var container = connection.inventoryManager().getContainer(packet.getContainerId());

    if (container == null) {
      log.debug(
        "Received container content update for unknown container {}", packet.getContainerId());
      return;
    }

    for (var i = 0; i < packet.getItems().length; i++) {
      container.setSlot(i, SFItemStack.from(packet.getItems()[i]));
    }
  }

  @EventHandler
  public void onSetContainerSlot(ClientboundContainerSetSlotPacket packet) {
    connection.inventoryManager().lastStateId(packet.getStateId());
    if (packet.getContainerId() == -1 && packet.getSlot() == -1) {
      connection.inventoryManager().cursorItem(SFItemStack.from(packet.getItem()));
      return;
    }

    var container = connection.inventoryManager().getContainer(packet.getContainerId());

    if (container == null) {
      log.debug("Received container slot update for unknown container {}", packet.getContainerId());
      return;
    }

    container.setSlot(packet.getSlot(), SFItemStack.from(packet.getItem()));
  }

  @EventHandler
  public void onSetContainerData(ClientboundContainerSetDataPacket packet) {
    var container = connection.inventoryManager().getContainer(packet.getContainerId());

    if (container == null) {
      log.debug("Received container data update for unknown container {}", packet.getContainerId());
      return;
    }

    container.setProperty(packet.getRawProperty(), packet.getValue());
  }

  @EventHandler
  public void onSetPlayerInventory(ClientboundSetPlayerInventoryPacket packet) {
    connection.inventoryManager().playerInventory().setSlot(packet.getSlot(), SFItemStack.from(packet.getContents()));
  }

  @EventHandler
  public void onSetCursor(ClientboundSetCursorItemPacket packet) {
    connection.inventoryManager().cursorItem(SFItemStack.from(packet.getContents()));
  }

  @EventHandler
  public void onSetSlot(ClientboundSetHeldSlotPacket packet) {
    localPlayer.inventory().selected = packet.getSlot();
  }

  @EventHandler
  public void onSetEquipment(ClientboundSetEquipmentPacket packet) {
    var entity = currentLevel().entityTracker().getEntity(packet.getEntityId());
    if (entity == null) {
      log.debug("Received equipment update for unknown entity {}", packet.getEntityId());
      return;
    }

    if (entity instanceof LivingEntity le) {
      for (var entry : packet.getEquipment()) {
        le.setItemSlot(EquipmentSlot.fromMCPl(entry.getSlot()), SFItemStack.from(entry.getItem()));
      }
    }
  }

  @EventHandler
  public void onOpenScreen(ClientboundOpenScreenPacket packet) {
    var container =
      new WindowContainer(packet.getType(), packet.getTitle(), packet.getContainerId());
    connection.inventoryManager().setContainer(packet.getContainerId(), container);
    connection.inventoryManager().currentContainer(container);
  }

  @EventHandler
  public void onOpenBookScreen(ClientboundOpenBookPacket packet) {}

  @EventHandler
  public void onOpenHorseScreen(ClientboundHorseScreenOpenPacket packet) {}

  @EventHandler
  public void onCloseContainer(ClientboundContainerClosePacket packet) {
    connection.inventoryManager().currentContainer(null);
  }

  @EventHandler
  public void onMapData(ClientboundMapItemDataPacket packet) {
    mapDataStates.computeIfAbsent(packet.getMapId(), k -> new MapDataState(packet)).update(packet);
  }

  @EventHandler
  public void onCooldown(ClientboundCooldownPacket packet) {
    if (packet.getCooldownTicks() == 0) {
      localPlayer.itemCoolDowns().removeInt(packet.getCooldownGroup());
    } else {
      localPlayer.itemCoolDowns().put(packet.getCooldownGroup(), packet.getCooldownTicks());
    }
  }

  @EventHandler
  public void onGameEvent(ClientboundGameEventPacket packet) {
    SFHelpers.mustSupply(() -> switch (packet.getNotification()) {
      case INVALID_BED -> () -> log.info("Bot had no bed/respawn anchor to respawn at (was maybe obstructed)");
      case START_RAIN -> () -> {
        level.levelData().raining(true);
        level.setRainLevel(1.0F);
      };
      case STOP_RAIN -> () -> {
        level.levelData().raining(false);
        level.setRainLevel(0.0F);
      };
      case CHANGE_GAMEMODE -> () -> gameModeState.setLocalMode(localPlayer, (GameMode) packet.getValue());
      case ENTER_CREDITS -> () -> {
        log.info("Entered credits {} (Respawning now)", packet.getValue());
        connection.sendPacket(
          new ServerboundClientCommandPacket(ClientCommand.RESPAWN)); // Respawns the player
      };
      case DEMO_MESSAGE -> () -> log.debug("Demo event: {}", packet.getValue());
      case ARROW_HIT_PLAYER -> () -> log.debug("Arrow hit player");
      case RAIN_STRENGTH -> () -> level.setRainLevel(((RainStrengthValue) packet.getValue()).getStrength());
      case THUNDER_STRENGTH -> () -> level.setThunderLevel(((ThunderStrengthValue) packet.getValue()).getStrength());
      case PUFFERFISH_STING_SOUND -> () -> log.debug("Pufferfish sting sound");
      case AFFECTED_BY_ELDER_GUARDIAN -> () -> log.debug("Affected by elder guardian");
      case ENABLE_RESPAWN_SCREEN -> () -> localPlayer.setShowDeathScreen(packet.getValue() == RespawnScreenValue.ENABLE_RESPAWN_SCREEN);
      case LIMITED_CRAFTING -> () -> localPlayer.setDoLimitedCrafting(packet.getValue() == LimitedCraftingValue.LIMITED_CRAFTING);
      case LEVEL_CHUNKS_LOAD_START -> () -> {
        log.debug("Level chunks load start");
        if (levelLoadStatusManager != null) {
          levelLoadStatusManager.loadingPacketsReceived();
        }
      };
    });
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

    var chunkData = level.chunks().getOrCreateChunkSection(packet.getX(), packet.getZ());

    for (var i = 0; i < chunkData.getSectionCount(); i++) {
      chunkData.setSection(i, MinecraftTypes.readChunkSection(buf));
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
        var biomePalette = MinecraftTypes.readDataPalette(buf, PaletteType.BIOME);
        chunkData.setSection(
          i, new ChunkSection(section.getBlockCount(), section.getChunkData(), biomePalette));
      }
    }
  }

  @EventHandler
  public void onChunkForget(ClientboundForgetLevelChunkPacket packet) {
    var level = currentLevel();
    level.chunks().removeChunkSection(packet.getX(), packet.getZ());
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
    var level = currentLevel();
    level.borderState(
      new BorderState(
        packet.getNewCenterX(),
        packet.getNewCenterZ(),
        packet.getOldSize(),
        packet.getNewSize(),
        packet.getLerpTime(),
        packet.getNewAbsoluteMaxSize(),
        packet.getWarningBlocks(),
        packet.getWarningTime())
    );
  }

  @EventHandler
  public void onBorderCenter(ClientboundSetBorderCenterPacket packet) {
    var level = currentLevel();
    level.borderState().centerX(packet.getNewCenterX());
    level.borderState().centerZ(packet.getNewCenterZ());
  }

  @EventHandler
  public void onBorderLerpSize(ClientboundSetBorderLerpSizePacket packet) {
    var level = currentLevel();
    level.borderState().oldSize(packet.getOldSize());
    level.borderState().newSize(packet.getNewSize());
    level.borderState().lerpTime(packet.getLerpTime());
  }

  @EventHandler
  public void onBorderSize(ClientboundSetBorderSizePacket packet) {
    var level = currentLevel();
    level.borderState().oldSize(level.borderState().newSize());
    level.borderState().newSize(packet.getSize());
  }

  @EventHandler
  public void onBorderWarningTime(ClientboundSetBorderWarningDelayPacket packet) {
    var level = currentLevel();
    level.borderState().warningTime(packet.getWarningDelay());
  }

  @EventHandler
  public void onBorderWarningBlocks(ClientboundSetBorderWarningDistancePacket packet) {
    var level = currentLevel();
    level.borderState().warningBlocks(packet.getWarningBlocks());
  }

  @EventHandler
  public void onEntitySpawn(ClientboundAddEntityPacket packet) {
    var level = currentLevel();
    EntityFactory.createEntity(
        connection,
        EntityType.REGISTRY.getById(packet.getType().ordinal()),
        currentLevel(),
        packet.getUuid())
      .ifPresent(entityState -> {
        entityState.fromAddEntityPacket(packet);
        level.entityTracker().addEntity(entityState);
      });
  }

  @EventHandler
  public void onExperienceOrbSpawn(ClientboundAddExperienceOrbPacket packet) {
    var level = currentLevel();
    var x = packet.getX();
    var y = packet.getY();
    var z = packet.getZ();
    var orb =
      new ExperienceOrbEntity(level, packet.getExp());
    orb.setPos(x, y, z);
    orb.syncPacketPositionCodec(x, y, z);
    orb.setYRot(0.0F);
    orb.setXRot(0.0F);
    orb.entityId(packet.getEntityId());
    orb.setPos(packet.getX(), packet.getY(), packet.getZ());

    level.entityTracker().addEntity(orb);
  }

  @EventHandler
  public void onEntityRemove(ClientboundRemoveEntitiesPacket packet) {
    var level = currentLevel();
    for (var entityId : packet.getEntityIds()) {
      level.entityTracker().removeEntity(entityId);
    }
  }

  @EventHandler
  public void onEntityMetadata(ClientboundSetEntityDataPacket packet) {
    var level = currentLevel();
    var state = level.entityTracker().getEntity(packet.getEntityId());

    if (state == null) {
      log.debug("Received entity metadata packet for unknown entity {}", packet.getEntityId());
      return;
    }

    for (var entry : packet.getMetadata()) {
      state.metadataState().setMetadata(entry);
    }
  }

  @EventHandler
  public void onEntityAttributes(ClientboundUpdateAttributesPacket packet) {
    var level = currentLevel();
    var state = level.entityTracker().getEntity(packet.getEntityId());

    if (state == null) {
      log.debug("Received entity attributes packet for unknown entity {}", packet.getEntityId());
      return;
    }

    for (var entry : packet.getAttributes()) {
      var key = entry.getType().getIdentifier();
      var attributeType = AttributeType.REGISTRY.getByKey(key);
      if (attributeType == null) {
        log.debug("Received unknown attribute type {}", key);
        continue;
      }

      var attribute =
        state.attributeState().getOrCreateAttribute(attributeType).baseValue(entry.getValue());

      attribute.modifiers().clear();
      attribute
        .modifiers()
        .putAll(
          entry.getModifiers()
            .stream()
            .collect(Collectors.toMap(AttributeModifier::getId, Function.identity())));
    }
  }

  @EventHandler
  public void onEntityEvent(ClientboundEntityEventPacket packet) {
    var level = currentLevel();
    var state = level.entityTracker().getEntity(packet.getEntityId());

    if (state == null) {
      log.debug("Received entity event packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.handleEntityEvent(packet.getEvent());
  }

  @EventHandler
  public void onUpdateEffect(ClientboundUpdateMobEffectPacket packet) {
    var level = currentLevel();
    var state = level.entityTracker().getEntity(packet.getEntityId());

    if (state == null) {
      log.debug("Received update effect packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state
      .effectState()
      .updateEffect(
        EffectType.REGISTRY.getById(packet.getEffect().ordinal()),
        packet.getAmplifier(),
        packet.getDuration(),
        packet.isAmbient(),
        packet.isShowParticles(),
        packet.isShowIcon(),
        packet.isBlend());
  }

  @EventHandler
  public void onRemoveEffect(ClientboundRemoveMobEffectPacket packet) {
    var level = currentLevel();
    var state = level.entityTracker().getEntity(packet.getEntityId());

    if (state == null) {
      log.debug("Received remove effect packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.effectState().removeEffect(EffectType.REGISTRY.getById(packet.getEffect().ordinal()));
  }

  @EventHandler
  public void onEntityMotion(ClientboundSetEntityMotionPacket packet) {
    var level = currentLevel();
    var state = level.entityTracker().getEntity(packet.getEntityId());

    if (state == null) {
      log.debug("Received entity motion packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.lerpMotion(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
  }

  @EventHandler
  public void onEntityPos(ClientboundMoveEntityPosPacket packet) {
    var level = currentLevel();
    var state = level.entityTracker().getEntity(packet.getEntityId());

    if (state == null) {
      log.debug("Received entity position packet for unknown entity {}", packet.getEntityId());
      return;
    }

    if (state.isControlledByLocalInstance()) {
      var newPos = state.getPositionCodec().decode(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ());
      state.getPositionCodec().base(newPos);
    } else {
      var newPos = state.getPositionCodec().decode(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ());
      state.getPositionCodec().base(newPos);
      state.setPos(newPos);
      state.setOnGround(packet.isOnGround());
    }
  }

  @EventHandler
  public void onEntityRot(ClientboundMoveEntityRotPacket packet) {
    var level = currentLevel();
    var state = level.entityTracker().getEntity(packet.getEntityId());

    if (state == null) {
      log.debug("Received entity rotation packet for unknown entity {}", packet.getEntityId());
      return;
    }

    if (!state.isControlledByLocalInstance()) {
      state.setRot(packet.getYaw(), packet.getPitch());
      state.setOnGround(packet.isOnGround());
    }
  }

  @EventHandler
  public void onEntityRot(ClientboundRotateHeadPacket packet) {
    var level = currentLevel();
    var state = level.entityTracker().getEntity(packet.getEntityId());

    if (state == null) {
      log.debug("Received entity head rotation packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.setHeadRotation(packet.getHeadYaw());
  }

  @EventHandler
  public void onEntityPosRot(ClientboundMoveEntityPosRotPacket packet) {
    var level = currentLevel();
    var state = level.entityTracker().getEntity(packet.getEntityId());

    if (state == null) {
      log.debug(
        "Received entity position rotation packet for unknown entity {}", packet.getEntityId());
      return;
    }

    if (state.isControlledByLocalInstance()) {
      var newPos = state.getPositionCodec().decode(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ());
      state.getPositionCodec().base(newPos);
    } else {
      var newPos = state.getPositionCodec().decode(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ());
      state.getPositionCodec().base(newPos);
      state.setPos(newPos);
      state.setRot(packet.getYaw(), packet.getPitch());
      state.setOnGround(packet.isOnGround());
    }
  }

  @EventHandler
  public void onEntityTeleport(ClientboundTeleportEntityPacket packet) {
    var level = currentLevel();
    var state = level.entityTracker().getEntity(packet.getId());

    if (state == null) {
      log.debug("Received entity teleport packet for unknown entity {}", packet.getId());
      return;
    }

    var horizontalAbsolute = packet.getRelatives().contains(PositionElement.X)
      || packet.getRelatives().contains(PositionElement.Y)
      || packet.getRelatives().contains(PositionElement.Z);
    var canLerp = !state.isControlledByLocalInstance() || horizontalAbsolute;
    setValuesFromPositionPacket(new EntityMovement(
      packet.getPosition(),
      packet.getDeltaMovement(),
      packet.getYRot(),
      packet.getXRot()
    ), packet.getRelatives(), state, canLerp);
    state.setOnGround(packet.isOnGround());
  }

  @EventHandler
  public void onEntityPositionSync(ClientboundEntityPositionSyncPacket packet) {
    var level = currentLevel();
    var state = level.entityTracker().getEntity(packet.getId());

    if (state == null) {
      log.debug("Received entity teleport packet for unknown entity {}", packet.getId());
      return;
    }

    state.getPositionCodec().base(packet.getPosition());
    if (!state.isControlledByLocalInstance()) {
      var yRot = packet.getYRot();
      var xRot = packet.getXRot();
      state.moveTo(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(), yRot, xRot);

      state.setOnGround(packet.isOnGround());
    }
  }

  @EventHandler
  public void onExplosion(ClientboundExplodePacket packet) {
    if (packet.getPlayerKnockback() != null) {
      localPlayer.addDeltaMovement(packet.getPlayerKnockback());
    }
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
    var plainMessage = SoulFireAdventure.PLAIN_MESSAGE_SERIALIZER.serialize(packet.getReason());
    log.error("Login failed with reason \"{}\"", plainMessage);

    handleTips(plainMessage);
  }

  @EventHandler
  public void onDisconnectPacket(ClientboundDisconnectPacket packet) {
    var plainMessage = SoulFireAdventure.PLAIN_MESSAGE_SERIALIZER.serialize(packet.getReason());
    log.info("Disconnected with reason \"{}\"", plainMessage);

    handleTips(plainMessage);
  }

  public void onDisconnectEvent(DisconnectedEvent event) {
    var reason = SoulFireAdventure.PLAIN_MESSAGE_SERIALIZER.serialize(event.getReason());
    var cause = event.getCause();
    if (cause == null) { // Packet wise disconnects have no cause
      log.info("Disconnected: {}", reason);
      return;
    }

    if (cause.getClass() == UnexpectedEncryptionException.class) {
      log.error("Server is online mode!", cause);
    } else if (reason.contains("Connection refused")) {
      log.error("Server is not reachable!", cause);
    } else {
      log.error("Disconnected", new Exception(reason, cause));
    }
  }

  private void handleTips(String message) {
    var lowerCaseMessage = message.toLowerCase(Locale.ROOT);
    if (lowerCaseMessage.contains("connection throttled")) {
      log.info("Tip: The server limits the amount of connections per second. To disable this, set 'settings.connection-throttle' to 0 in bukkit.yml of the server.");
      log.info("Tip: If you don't have access to the server, you can try increasing your join delay in the bot settings.");
    }
  }

  public @NotNull Level currentLevel() {
    return Objects.requireNonNull(level, "Current level is not set");
  }

  public boolean isLevelRunningNormally() {
    return this.level == null || this.level.tickRateManager().runsNormally();
  }

  public void tick() {
    if (this.level != null) {
      this.level.tickRateManager().tick();
    }

    if (this.level != null) {
      this.gameModeState.tick();
    }

    if (this.levelLoadStatusManager != null) {
      this.levelLoadStatusManager.tick();
      if (this.levelLoadStatusManager.levelReady() && !this.localPlayer.hasClientLoaded()) {
        this.connection.sendPacket(ServerboundPlayerLoadedPacket.INSTANCE);
        this.localPlayer.setClientLoaded(true);
      }
    }

    this.connection.session().flush();

    if (this.level != null) {
      this.level.tickEntities();
      this.level.tick();
    }
  }
}
