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

import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.BotJoinedEvent;
import com.soulfiremc.server.api.event.bot.BotPostEntityTickEvent;
import com.soulfiremc.server.api.event.bot.BotPreEntityTickEvent;
import com.soulfiremc.server.api.event.bot.ChatMessageReceiveEvent;
import com.soulfiremc.server.data.*;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.BuiltInKnownPackRegistry;
import com.soulfiremc.server.protocol.SFProtocolConstants;
import com.soulfiremc.server.protocol.SFProtocolHelper;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.container.WindowContainer;
import com.soulfiremc.server.protocol.bot.model.*;
import com.soulfiremc.server.protocol.bot.state.*;
import com.soulfiremc.server.protocol.bot.state.entity.EntityFactory;
import com.soulfiremc.server.protocol.bot.state.entity.ExperienceOrbEntity;
import com.soulfiremc.server.protocol.bot.state.entity.LivingEntity;
import com.soulfiremc.server.protocol.bot.state.entity.LocalPlayer;
import com.soulfiremc.server.protocol.bot.state.registry.Biome;
import com.soulfiremc.server.protocol.bot.state.registry.DimensionType;
import com.soulfiremc.server.protocol.bot.state.registry.SFChatType;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.server.util.EntityMovement;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.structs.TickTimer;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.data.UnexpectedEncryptionException;
import org.geysermc.mcprotocollib.protocol.data.game.*;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatType;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.PaletteType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeModifier;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.GlobalPos;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
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
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginFinishedPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@ToString
public final class SessionDataManager {
  private final SettingsSource settingsSource;
  private final Logger log;
  private final MinecraftCodecHelper codecHelper;
  private final BotConnection connection;
  private final WeatherState weatherState = new WeatherState();
  private final PlayerListState playerListState = new PlayerListState();
  private final Object2IntMap<Key> itemCoolDowns = Object2IntMaps.synchronize(new Object2IntOpenHashMap<>());
  private final Map<ResourceKey<?>, List<RegistryEntry>> resolvedRegistryData = new LinkedHashMap<>();
  private final Registry<DimensionType> dimensionTypeRegistry = new Registry<>(RegistryKeys.DIMENSION_TYPE);
  private final Registry<Biome> biomeRegistry = new Registry<>(RegistryKeys.BIOME);
  private final Registry<SFChatType> chatTypeRegistry = new Registry<>(RegistryKeys.CHAT_TYPE);
  private final Int2ObjectMap<MapDataState> mapDataStates = new Int2ObjectOpenHashMap<>();
  private final EntityTrackerState entityTrackerState = new EntityTrackerState();
  private final TagsState tagsState = new TagsState();
  private Key[] serverEnabledFeatures;
  private List<KnownPack> serverKnownPacks;
  private LocalPlayer localPlayer;
  private @Nullable ServerPlayData serverPlayData;
  private BorderState borderState;
  private HealthData healthData;
  private GameMode gameMode = null;
  private @Nullable GameMode previousGameMode = null;
  private GameProfile botProfile;
  private LoginPacketData loginData;
  private boolean enableRespawnScreen;
  private boolean doLimitedCrafting;
  @Getter(value = AccessLevel.PRIVATE)
  private Level level;
  private final TickTimer tickTimer = new TickTimer(20.0F, 0L, this::getTickTargetMillis);
  private int serverViewDistance = -1;
  private int serverSimulationDistance = -1;
  private @Nullable GlobalPos lastDeathPos;
  private int portalCooldown = -1;
  private @Nullable DifficultyData difficultyData;
  private @Nullable DefaultSpawnData defaultSpawnData;
  private @Nullable ExperienceData experienceData;
  private @Nullable ChunkKey centerChunk;
  private boolean isDead = false;
  private boolean joinedWorld = false;
  private String serverBrand;

  public SessionDataManager(BotConnection connection) {
    this.settingsSource = connection.settingsSource();
    this.log = connection.logger();
    this.codecHelper = connection.session().getCodecHelper();
    this.connection = connection;
  }

  private static String toPlainText(Component component) {
    return SoulFireServer.PLAIN_MESSAGE_SERIALIZER.serialize(component);
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
      var lv = this.level.tickRateManager();
      if (lv.runsNormally()) {
        return Math.max(defaultValue, lv.millisecondsPerTick());
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
    // Set data from the packet
    loginData =
      new LoginPacketData(packet.isHardcore(), packet.getWorldNames(), packet.getMaxPlayers(), packet.isEnforcesSecureChat());

    enableRespawnScreen = packet.isEnableRespawnScreen();
    doLimitedCrafting = packet.isDoLimitedCrafting();
    serverViewDistance = packet.getViewDistance();
    serverSimulationDistance = packet.getSimulationDistance();

    processSpawnInfo(packet.getCommonPlayerSpawnInfo());

    // Init client entity
    localPlayer =
      new LocalPlayer(connection, currentLevel(), botProfile);
    localPlayer.entityId(packet.getEntityId());
    localPlayer.showReducedDebug(packet.isReducedDebugInfo());
    connection.inventoryManager().setContainer(0, localPlayer.inventory());

    entityTrackerState.addEntity(localPlayer);
  }

  private void processSpawnInfo(PlayerSpawnInfo spawnInfo) {
    level =
      new Level(
        tagsState,
        entityTrackerState,
        dimensionTypeRegistry.getById(spawnInfo.getDimension()),
        spawnInfo.getWorldName(),
        spawnInfo.getHashedSeed(),
        spawnInfo.isDebug(),
        spawnInfo.isFlat(),
        spawnInfo.getSeaLevel());
    gameMode = spawnInfo.getGameMode();
    previousGameMode = spawnInfo.getPreviousGamemode();
    lastDeathPos = spawnInfo.getLastDeathPos();
    portalCooldown = spawnInfo.getPortalCooldown();
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
    localPlayer.setFrom(new EntityMovement(
      packet.getPosition(),
      packet.getDeltaMovement(),
      packet.getYRot(),
      packet.getXRot()
    ), packet.getRelatives());

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

    connection.sendPacket(new ServerboundMovePlayerPosRotPacket(
      false,
      false,
      localPlayer.pos().getX(),
      localPlayer.pos().getY(),
      localPlayer.pos().getZ(),
      localPlayer.yRot(),
      localPlayer.xRot()
    ));
    connection.sendPacket(new ServerboundAcceptTeleportationPacket(packet.getId()));
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
      var entity = entityTrackerState.getEntity(packet.getTargetEntityId());
      if (entity != null) {
        targetPosition = entity.originPosition(packet.getTargetEntityOrigin());
      }
    }

    localPlayer.lookAt(packet.getOrigin(), targetPosition);
  }

  @EventHandler
  public void onRespawn(ClientboundRespawnPacket packet) {
    processSpawnInfo(packet.getCommonPlayerSpawnInfo());

    // We are now possibly in a new dimension
    localPlayer.level(currentLevel());

    log.info("Respawned");
  }

  @EventHandler
  public void onDeath(ClientboundPlayerCombatKillPacket packet) {
    var state = entityTrackerState.getEntity(packet.getPlayerId());

    if (state == null || state != localPlayer) {
      log.debug("Received death for unknown or invalid entity {}", packet.getPlayerId());
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
    var channelKey = packet.getChannel();
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
    for (var update : packet.getEntries()) {
      var entry = playerListState.entries().computeIfAbsent(update.getProfileId(), k -> update);
      for (var action : packet.getActions()) {
        SFHelpers.mustSupply(() -> switch (action) {
          case ADD_PLAYER -> () -> entry.setProfile(update.getProfile());
          case INITIALIZE_CHAT -> () -> {
            entry.setSessionId(update.getSessionId());
            entry.setExpiresAt(update.getExpiresAt());
            entry.setKeySignature(update.getKeySignature());
            entry.setPublicKey(update.getPublicKey());
          };
          case UPDATE_GAME_MODE -> () -> entry.setGameMode(update.getGameMode());
          case UPDATE_LISTED -> () -> entry.setListed(update.isListed());
          case UPDATE_LATENCY -> () -> entry.setLatency(update.getLatency());
          case UPDATE_DISPLAY_NAME -> () -> entry.setDisplayName(update.getDisplayName());
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
    serverViewDistance = packet.getViewDistance();
  }

  @EventHandler
  public void onSetDifficulty(ClientboundChangeDifficultyPacket packet) {
    difficultyData = new DifficultyData(packet.getDifficulty(), packet.isDifficultyLocked());
  }

  @EventHandler
  public void onAbilities(ClientboundPlayerAbilitiesPacket packet) {
    var abilitiesData = localPlayer.abilitiesData();
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

    level.gameTime(packet.getGameTime());
    level.dayTime(packet.getDayTime());
    level.tickDayTime(packet.isTickDayTime());
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
    var entity = entityTrackerState.getEntity(packet.getEntityId());
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
      itemCoolDowns.removeInt(packet.getCooldownGroup());
    } else {
      itemCoolDowns.put(packet.getCooldownGroup(), packet.getCooldownTicks());
    }
  }

  @EventHandler
  public void onGameEvent(ClientboundGameEventPacket packet) {
    SFHelpers.mustSupply(() -> switch (packet.getNotification()) {
      case INVALID_BED -> () -> log.info("Bot had no bed/respawn anchor to respawn at (was maybe obstructed)");
      case START_RAIN -> () -> weatherState.raining(true);
      case STOP_RAIN -> () -> weatherState.raining(false);
      case CHANGE_GAMEMODE -> () -> {
        previousGameMode = gameMode;
        gameMode = (GameMode) packet.getValue();
      };
      case ENTER_CREDITS -> () -> {
        log.info("Entered credits {} (Respawning now)", packet.getValue());
        connection.sendPacket(
          new ServerboundClientCommandPacket(ClientCommand.RESPAWN)); // Respawns the player
      };
      case DEMO_MESSAGE -> () -> log.debug("Demo event: {}", packet.getValue());
      case ARROW_HIT_PLAYER -> () -> log.debug("Arrow hit player");
      case RAIN_STRENGTH -> () -> weatherState.rainStrength(((RainStrengthValue) packet.getValue()).getStrength());
      case THUNDER_STRENGTH -> () -> weatherState.thunderStrength(((ThunderStrengthValue) packet.getValue()).getStrength());
      case PUFFERFISH_STING_SOUND -> () -> log.debug("Pufferfish sting sound");
      case AFFECTED_BY_ELDER_GUARDIAN -> () -> log.debug("Affected by elder guardian");
      case ENABLE_RESPAWN_SCREEN -> () -> enableRespawnScreen = packet.getValue() == RespawnScreenValue.ENABLE_RESPAWN_SCREEN;
      case LIMITED_CRAFTING -> () -> doLimitedCrafting = packet.getValue() == LimitedCraftingValue.LIMITED_CRAFTING;
      case LEVEL_CHUNKS_LOAD_START -> () -> log.debug("Level chunks load start");
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
    var entityState = EntityFactory.createEntity(
      connection,
      EntityType.REGISTRY.getById(packet.getType().ordinal()),
      currentLevel(),
      packet.getUuid());
    entityState.fromAddEntityPacket(packet);

    entityTrackerState.addEntity(entityState);
  }

  @EventHandler
  public void onExperienceOrbSpawn(ClientboundAddExperienceOrbPacket packet) {
    var x = packet.getX();
    var y = packet.getY();
    var z = packet.getZ();
    var orb =
      new ExperienceOrbEntity(currentLevel(), packet.getExp());
    orb.setPos(x, y, z);
    orb.syncPacketPositionCodec(x, y, z);
    orb.setYRot(0.0F);
    orb.setXRot(0.0F);
    orb.entityId(packet.getEntityId());
    orb.setPos(packet.getX(), packet.getY(), packet.getZ());

    entityTrackerState.addEntity(orb);
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
      log.debug("Received entity metadata packet for unknown entity {}", packet.getEntityId());
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
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.debug("Received entity event packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.handleEntityEvent(packet.getEvent());
  }

  @EventHandler
  public void onUpdateEffect(ClientboundUpdateMobEffectPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

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
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.debug("Received remove effect packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.effectState().removeEffect(EffectType.REGISTRY.getById(packet.getEffect().ordinal()));
  }

  @EventHandler
  public void onEntityMotion(ClientboundSetEntityMotionPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.debug("Received entity motion packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.setDeltaMovement(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
  }

  @EventHandler
  public void onEntityPos(ClientboundMoveEntityPosPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

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
    var state = entityTrackerState.getEntity(packet.getEntityId());

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
    var state = entityTrackerState.getEntity(packet.getEntityId());

    if (state == null) {
      log.debug("Received entity head rotation packet for unknown entity {}", packet.getEntityId());
      return;
    }

    state.setHeadRotation(packet.getHeadYaw());
  }

  @EventHandler
  public void onEntityPosRot(ClientboundMoveEntityPosRotPacket packet) {
    var state = entityTrackerState.getEntity(packet.getEntityId());

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
    var state = entityTrackerState.getEntity(packet.getId());

    if (state == null) {
      log.debug("Received entity teleport packet for unknown entity {}", packet.getId());
      return;
    }

    state.setFrom(new EntityMovement(
      packet.getPosition(),
      packet.getDeltaMovement(),
      packet.getYRot(),
      packet.getXRot()
    ), packet.getRelatives());
    state.setOnGround(packet.isOnGround());
  }

  @EventHandler
  public void onEntityPositionSync(ClientboundEntityPositionSyncPacket packet) {
    var state = entityTrackerState.getEntity(packet.getId());

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
      log.info("Disconnected: {}", reason);
      return;
    }

    if (cause.getClass() == UnexpectedEncryptionException.class) {
      log.error("Server is online mode!", cause);
    } else if (reason.contains("Connection refused")) {
      log.error("Server is not reachable!", cause);
    } else {
      log.error("Disconnected: %s".formatted(reason), cause);
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

  public void tick() {
    if (this.level != null) {
      this.level.tickRateManager().tick();
    }

    // Tick border changes
    if (borderState != null) {
      borderState.tick();
    }

    // Tick cooldowns
    tickCooldowns();

    SoulFireAPI.postEvent(new BotPreEntityTickEvent(connection));

    // Tick entities
    entityTrackerState.tick();

    SoulFireAPI.postEvent(new BotPostEntityTickEvent(connection));
  }

  private void tickCooldowns() {
    if (portalCooldown > 0) {
      portalCooldown--;
    }

    synchronized (itemCoolDowns) {
      var iterator = itemCoolDowns.object2IntEntrySet().iterator();
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
