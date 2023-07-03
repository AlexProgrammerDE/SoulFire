/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.protocol.bot;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.UnexpectedEncryptionException;
import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.data.game.ResourcePackStatus;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkBiomeData;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.github.steveice10.mc.protocol.data.game.chunk.DataPalette;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.PaletteType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.GlobalPos;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.PositionElement;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockChangeEntry;
import com.github.steveice10.mc.protocol.data.game.level.notify.RainStrengthValue;
import com.github.steveice10.mc.protocol.data.game.level.notify.RespawnScreenValue;
import com.github.steveice10.mc.protocol.data.game.level.notify.ThunderStrengthValue;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddExperienceOrbPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.border.*;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundResourcePackPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.ToString;
import net.kyori.adventure.text.Component;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.bot.BotPostTickEvent;
import net.pistonmaster.serverwrecker.api.event.bot.BotPreTickEvent;
import net.pistonmaster.serverwrecker.api.event.bot.ChatMessageReceiveEvent;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.bot.container.Container;
import net.pistonmaster.serverwrecker.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.serverwrecker.protocol.bot.container.WindowContainer;
import net.pistonmaster.serverwrecker.protocol.bot.model.*;
import net.pistonmaster.serverwrecker.protocol.bot.state.*;
import net.pistonmaster.serverwrecker.protocol.bot.state.entity.EntityLikeState;
import net.pistonmaster.serverwrecker.protocol.bot.state.entity.EntityState;
import net.pistonmaster.serverwrecker.protocol.bot.state.entity.ExperienceOrbState;
import net.pistonmaster.serverwrecker.protocol.bot.state.entity.PlayerState;
import net.pistonmaster.serverwrecker.protocol.netty.ViaClientSession;
import net.pistonmaster.serverwrecker.settings.lib.SettingsHolder;
import net.pistonmaster.serverwrecker.util.BusHandler;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@ToString
public final class SessionDataManager {
    private final SettingsHolder settingsHolder;
    private final Logger log;
    private final ServerWrecker serverWrecker;
    private final ViaClientSession session;
    private final BotConnection connection;
    private final WeatherState weatherState = new WeatherState();
    private final PlayerListState playerListState = new PlayerListState();
    private final Map<Integer, AtomicInteger> itemCoolDowns = new ConcurrentHashMap<>();
    private final Map<String, LevelState> levels = new ConcurrentHashMap<>();
    private final Int2ObjectMap<BiomeData> biomes = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<MapDataState> mapDataStates = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Container> containerData = new Int2ObjectOpenHashMap<>();
    private final EntityTrackerState entityTrackerState = new EntityTrackerState();
    private final EntityMetadataState selfMetaData = new EntityMetadataState();
    private BorderState borderState;
    private BotMovementManager botMovementManager;
    private HealthData healthData;
    private GameMode gameMode = null;
    private @Nullable GameMode previousGameMode = null;
    private GameProfile botProfile;
    private LoginPacketData loginData;
    private boolean doImmediateRespawn;
    private DimensionData currentDimension;
    private int serverViewDistance = -1;
    private int serverSimulationDistance = -1;
    private @Nullable GlobalPos lastDeathPos;
    private @Nullable DifficultyData difficultyData;
    private @Nullable AbilitiesData abilitiesData;
    private @Nullable DefaultSpawnData defaultSpawnData;
    private @Nullable ExperienceData experienceData;
    private int openContainerId = -1;
    private int heldItemSlot = -1;
    private int biomesEntryBitsSize = -1;
    private @Nullable ChunkKey centerChunk;
    private boolean isDead = false;

    public SessionDataManager(BotConnection connection) {
        this.settingsHolder = connection.settingsHolder();
        this.log = connection.logger();
        this.serverWrecker = connection.serverWrecker();
        this.session = connection.session();
        this.connection = connection;
    }

    @BusHandler
    public void onLoginSuccess(ClientboundGameProfilePacket packet) {
        botProfile = packet.getProfile();
    }

    @BusHandler
    public void onJoin(ClientboundLoginPacket packet) {
        loginData = new LoginPacketData(
                packet.getEntityId(),
                packet.isHardcore(),
                packet.getWorldNames(),
                packet.getRegistry(),
                packet.getMaxPlayers(),
                packet.isReducedDebugInfo()
        );
        CompoundTag dimensionRegistry = loginData.registry().get("minecraft:dimension_type");
        for (Tag type : dimensionRegistry.<ListTag>get("value").getValue()) {
            CompoundTag dimension = (CompoundTag) type;
            String name = dimension.<StringTag>get("name").getValue();
            int id = dimension.<IntTag>get("id").getValue();

            levels.put(name, new LevelState(this, name, id, dimension.get("element")));
        }
        CompoundTag biomeRegistry = loginData.registry().get("minecraft:worldgen/biome");
        for (Tag type : biomeRegistry.<ListTag>get("value").getValue()) {
            CompoundTag biome = (CompoundTag) type;
            BiomeData biomeData = new BiomeData(biome);

            biomes.put(biomeData.id(), biomeData);
        }
        biomesEntryBitsSize = ChunkData.log2RoundUp(biomes.size());

        doImmediateRespawn = !packet.isEnableRespawnScreen();
        currentDimension = new DimensionData(
                packet.getDimension(),
                packet.getWorldName(),
                packet.getHashedSeed(),
                packet.isDebug(),
                packet.isFlat()
        );
        gameMode = packet.getGameMode();
        previousGameMode = packet.getPreviousGamemode();
        serverViewDistance = packet.getViewDistance();
        serverSimulationDistance = packet.getSimulationDistance();
        lastDeathPos = packet.getLastDeathPos();

        containerData.put(0, new PlayerInventoryContainer());
    }

    @BusHandler
    public void onPosition(ClientboundPlayerPositionPacket packet) {
        double currentX = botMovementManager != null ? botMovementManager.getX() : 0;
        double currentY = botMovementManager != null ? botMovementManager.getY() : 0;
        double currentZ = botMovementManager != null ? botMovementManager.getZ() : 0;
        float currentYaw = botMovementManager != null ? botMovementManager.getYaw() : 0;
        float currentPitch = botMovementManager != null ? botMovementManager.getPitch() : 0;

        boolean xRelative = packet.getRelative().contains(PositionElement.X);
        boolean yRelative = packet.getRelative().contains(PositionElement.Y);
        boolean zRelative = packet.getRelative().contains(PositionElement.Z);
        boolean yawRelative = packet.getRelative().contains(PositionElement.YAW);
        boolean pitchRelative = packet.getRelative().contains(PositionElement.PITCH);

        double x = xRelative ? currentX + packet.getX() : packet.getX();
        double y = yRelative ? currentY + packet.getY() : packet.getY();
        double z = zRelative ? currentZ + packet.getZ() : packet.getZ();
        float yaw = yawRelative ? currentYaw + packet.getYaw() : packet.getYaw();
        float pitch = pitchRelative ? currentPitch + packet.getPitch() : packet.getPitch();

        if (botMovementManager == null) {
            botMovementManager = new BotMovementManager(this, x, y, z, yaw, pitch);
            log.info("Joined server at position: X {} Y {} Z {}", Math.round(x), Math.round(y), Math.round(z));
        } else {
            botMovementManager.setPosition(x, y, z);
            botMovementManager.setRotation(yaw, pitch);
        }

        session.send(new ServerboundAcceptTeleportationPacket(packet.getTeleportId()));

        log.debug("Position updated: {}", botMovementManager);
    }

    @BusHandler
    public void onLookAt(ClientboundPlayerLookAtPacket packet) {
        botMovementManager.lookAt(packet.getOrigin(),
                Vector3d.from(packet.getX(), packet.getY(), packet.getZ()));

        // TODO: Implement entity look at
    }

    @BusHandler
    public void onRespawn(ClientboundRespawnPacket packet) {
        currentDimension = new DimensionData(
                packet.getDimension(),
                packet.getWorldName(),
                packet.getHashedSeed(),
                packet.isDebug(),
                packet.isFlat()
        );
        gameMode = packet.getGamemode();
        previousGameMode = packet.getPreviousGamemode();
        lastDeathPos = packet.getLastDeathPos();

        log.info("Respawned");
    }

    @BusHandler
    public void onDeath(ClientboundPlayerCombatKillPacket packet) {
        this.isDead = true;
    }

    //
    // Chat packets
    //

    @BusHandler
    public void onPlayerChat(ClientboundPlayerChatPacket packet) {
        Component message = packet.getUnsignedContent();
        if (message != null) {
            onChat(message);
            return;
        }

        onChat(Component.text(packet.getContent()));

        /*
        System.out.println(getCurrentLevel().getBlockTypeAt(botMovementManager.getBlockPos()));
        RouteFinder routeFinder = new RouteFinder(new MinecraftGraph(this), new MovementScorer(), new MovementScorer());
        BlockPosition start = new BlockPosition(botMovementManager.getPlayerPos());
        BlockPosition target = new BlockPosition(botMovementManager.getPlayerPos().add(4, 0, 2));
        System.out.println("Start: " + start);
        System.out.println("Target: " + target);
        List<BlockPosition> actions = routeFinder.findRoute(start, target);
        System.out.println(actions);
        BlockPosition last = start;
        for (BlockPosition action : actions) {
            System.out.println("Move: " + action.position().sub(last.position()));
            last = action;
        }
         */
    }

    @BusHandler
    public void onServerChat(ClientboundSystemChatPacket packet) {
        onChat(packet.getContent());
    }

    @BusHandler
    public void onDisguisedChat(ClientboundDisguisedChatPacket packet) {
    }

    private void onChat(Component message) {
        ServerWreckerAPI.postEvent(new ChatMessageReceiveEvent(connection, message));
    }

    //
    // Player list packets
    //

    @BusHandler
    public void onPlayerListHeaderFooter(ClientboundTabListPacket packet) {
        playerListState.setHeader(packet.getHeader());
        playerListState.setFooter(packet.getFooter());
    }

    @BusHandler
    public void onPlayerListUpdate(ClientboundPlayerInfoUpdatePacket packet) {
        for (PlayerListEntry update : packet.getEntries()) {
            PlayerListEntry entry = playerListState.getEntries().computeIfAbsent(update.getProfileId(), k -> update);
            for (PlayerListEntryAction action : packet.getActions()) {
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

    @BusHandler
    public void onPlayerListRemove(ClientboundPlayerInfoRemovePacket packet) {
        for (UUID profileId : packet.getProfileIds()) {
            playerListState.getEntries().remove(profileId);
        }
    }

    //
    // Player data packets
    //

    @BusHandler
    public void onSetSimulationDistance(ClientboundSetSimulationDistancePacket packet) {
        serverSimulationDistance = packet.getSimulationDistance();
    }

    @BusHandler
    public void onSetViewDistance(ClientboundSetChunkCacheRadiusPacket packet) {
        serverViewDistance = packet.getViewDistance();
    }

    @BusHandler
    public void onSetDifficulty(ClientboundChangeDifficultyPacket packet) {
        difficultyData = new DifficultyData(packet.getDifficulty(), packet.isDifficultyLocked());
    }

    @BusHandler
    public void onAbilities(ClientboundPlayerAbilitiesPacket packet) {
        abilitiesData = new AbilitiesData(packet.isInvincible(), packet.isFlying(), packet.isCanFly(), packet.isCreative(), packet.getFlySpeed(), packet.getWalkSpeed());
    }

    @BusHandler
    public void onCompassTarget(ClientboundSetDefaultSpawnPositionPacket packet) {
        defaultSpawnData = new DefaultSpawnData(packet.getPosition(), packet.getAngle());
    }

    @BusHandler
    public void onHealth(ClientboundSetHealthPacket packet) {
        this.healthData = new HealthData(packet.getHealth(), packet.getFood(), packet.getSaturation());

        if (healthData.health() < 1) {
            this.isDead = true;
        }

        log.debug("Health updated: {}", healthData);
    }

    @BusHandler
    public void onExperience(ClientboundSetExperiencePacket packet) {
        experienceData = new ExperienceData(packet.getExperience(), packet.getLevel(), packet.getTotalExperience());
    }

    @BusHandler
    public void onMapData(ClientboundMapItemDataPacket packet) {
        mapDataStates.computeIfAbsent(packet.getMapId(), k -> new MapDataState()).update(packet);
    }

    //
    // Inventory packets
    //

    @BusHandler
    public void onSetContainerContent(ClientboundContainerSetContentPacket packet) {
        Container container = containerData.get(packet.getContainerId());

        if (container == null) {
            log.warn("Received container content update for unknown container {}", packet.getContainerId());
            return;
        }

        for (int i = 0; i < packet.getItems().length; i++) {
            container.setSlot(i, packet.getItems()[i]);
        }
    }

    @BusHandler
    public void onSetContainerSlot(ClientboundContainerSetSlotPacket packet) {
        Container container = containerData.get(packet.getContainerId());

        if (container == null) {
            log.warn("Received container slot update for unknown container {}", packet.getContainerId());
            return;
        }

        container.setSlot(packet.getSlot(), packet.getItem());
    }

    @BusHandler
    public void onSetContainerData(ClientboundContainerSetDataPacket packet) {
        Container container = containerData.get(packet.getContainerId());

        if (container == null) {
            log.warn("Received container data update for unknown container {}", packet.getContainerId());
            return;
        }

        container.setProperty(packet.getRawProperty(), packet.getValue());
    }

    @BusHandler
    public void onSetSlot(ClientboundSetCarriedItemPacket packet) {
        heldItemSlot = packet.getSlot();
    }

    @BusHandler
    public void onOpenScreen(ClientboundOpenScreenPacket packet) {
        containerData.put(packet.getContainerId(), new WindowContainer(packet.getType(), packet.getTitle(), packet.getContainerId()));
        openContainerId = packet.getContainerId();
    }

    @BusHandler
    public void onOpenBookScreen(ClientboundOpenBookPacket packet) {
    }

    @BusHandler
    public void onOpenHorseScreen(ClientboundHorseScreenOpenPacket packet) {
    }

    @BusHandler
    public void onCloseContainer(ClientboundContainerClosePacket packet) {
        openContainerId = -1;
    }

    @BusHandler
    public void onExperience(ClientboundCooldownPacket packet) {
        if (packet.getCooldownTicks() == 0) {
            itemCoolDowns.remove(packet.getItemId());
        } else {
            itemCoolDowns.put(packet.getItemId(), new AtomicInteger(packet.getCooldownTicks()));
        }
    }

    @BusHandler
    public void onGameEvent(ClientboundGameEventPacket packet) {
        switch (packet.getNotification()) {
            case INVALID_BED -> log.info("Bot had no bed/respawn anchor to respawn at (was maybe obstructed)");
            case START_RAIN -> weatherState.setRaining(true);
            case STOP_RAIN -> weatherState.setRaining(false);
            case CHANGE_GAMEMODE -> {
                previousGameMode = gameMode;
                gameMode = (GameMode) packet.getValue();
            }
            case ENTER_CREDITS -> {
                log.info("Entered credits {} (Respawning now)", packet.getValue());
                session.send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN)); // Respawns the player
            }
            case DEMO_MESSAGE -> log.debug("Demo event: {}", packet.getValue());
            case ARROW_HIT_PLAYER -> log.debug("Arrow hit player");
            case RAIN_STRENGTH -> weatherState.setRainStrength(((RainStrengthValue) packet.getValue()).getStrength());
            case THUNDER_STRENGTH ->
                    weatherState.setThunderStrength(((ThunderStrengthValue) packet.getValue()).getStrength());
            case PUFFERFISH_STING_SOUND -> log.debug("Pufferfish sting sound");
            case AFFECTED_BY_ELDER_GUARDIAN -> log.debug("Affected by elder guardian");
            case ENABLE_RESPAWN_SCREEN ->
                    doImmediateRespawn = packet.getValue() == RespawnScreenValue.IMMEDIATE_RESPAWN;
        }
    }

    @BusHandler
    public void onGameEvent(ClientboundSetChunkCacheCenterPacket packet) {
        centerChunk = new ChunkKey(packet.getChunkX(), packet.getChunkZ());
    }

    @BusHandler
    public void onChunkForget(ClientboundForgetLevelChunkPacket packet) {
        LevelState level = getCurrentLevel();

        if (level == null) {
            log.warn("Received section update while not in a level");
            return;
        }

        level.getChunks().remove(new ChunkKey(packet.getX(), packet.getZ()));
    }

    @BusHandler
    public void onChunkData(ClientboundLevelChunkWithLightPacket packet) {
        MinecraftCodecHelper helper = session.getCodecHelper();
        LevelState level = getCurrentLevel();

        if (level == null) {
            log.warn("Received section update while not in a level");
            return;
        }

        ChunkKey key = new ChunkKey(packet.getX(), packet.getZ());
        byte[] data = packet.getChunkData();
        ByteBuf buf = Unpooled.wrappedBuffer(data);

        ChunkData chunkData = level.getChunks().computeIfAbsent(key, k -> new ChunkData(level));

        try {
            for (int i = 0; i < chunkData.getSections().length; i++) {
                chunkData.getSections()[i] = readChunkSection(buf, helper);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @BusHandler
    public void onSectionBlockUpdate(ClientboundSectionBlocksUpdatePacket packet) {
        ChunkKey key = new ChunkKey(packet.getChunkX(), packet.getChunkZ());
        LevelState level = getCurrentLevel();

        if (level == null) {
            log.warn("Received section update while not in a level");
            return;
        }

        ChunkData chunkData = level.getChunks().get(key);

        if (chunkData == null) {
            log.warn("Received section update for unknown chunk: {}", key);
            return;
        }

        for (BlockChangeEntry entry : packet.getEntries()) {
            Vector3i vector3i = entry.getPosition();
            int newId = entry.getBlock();

            log.debug("Updating block at {} to {}", vector3i, newId);
            level.setBlockId(vector3i, newId);
        }
    }

    @BusHandler
    public void onBlockUpdate(ClientboundBlockUpdatePacket packet) {
        LevelState level = getCurrentLevel();

        if (level == null) {
            log.warn("Received section update while not in a level");
            return;
        }

        BlockChangeEntry entry = packet.getEntry();

        Vector3i vector3i = entry.getPosition();
        int newId = entry.getBlock();

        level.setBlockId(vector3i, newId);

        log.debug("Updating block at {} to {}", vector3i, newId);
    }

    @BusHandler
    public void onChunkData(ClientboundChunksBiomesPacket packet) {
        LevelState level = getCurrentLevel();

        if (level == null) {
            log.warn("Received section update while not in a level");
            return;
        }

        MinecraftCodecHelper codec = session.getCodecHelper();

        for (ChunkBiomeData biomeData : packet.getChunkBiomeData()) {
            ChunkKey key = new ChunkKey(biomeData.getX(), biomeData.getZ());
            ChunkData chunkData = level.getChunks().get(key);

            if (chunkData == null) {
                log.warn("Received biome update for unknown chunk: {}", key);
                return;
            }

            ByteBuf buf = Unpooled.wrappedBuffer(biomeData.getBuffer());
            try {
                for (int i = 0; chunkData.getSections().length > i; i++) {
                    ChunkSection section = chunkData.getSections()[i];
                    DataPalette biomePalette = codec.readDataPalette(buf, PaletteType.BIOME, biomesEntryBitsSize);
                    chunkData.getSections()[i] = new ChunkSection(section.getBlockCount(), section.getChunkData(), biomePalette);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @BusHandler
    public void onBlockDestruction(ClientboundBlockDestructionPacket packet) {
        // Indicates the ten states of a block-breaking animation
    }

    @BusHandler
    public void onBlockChangedAck(ClientboundBlockChangedAckPacket packet) {
        // TODO: Implement block break
    }

    //
    // World border packets
    //

    @BusHandler
    public void onBorderInit(ClientboundInitializeBorderPacket packet) {
        borderState = new BorderState(packet.getNewCenterX(), packet.getNewCenterZ(), packet.getOldSize(), packet.getNewSize(),
                packet.getLerpTime(), packet.getNewAbsoluteMaxSize(), packet.getWarningBlocks(), packet.getWarningTime());
    }

    @BusHandler
    public void onBorderCenter(ClientboundSetBorderCenterPacket packet) {
        borderState.setCenterX(packet.getNewCenterX());
        borderState.setCenterZ(packet.getNewCenterZ());
    }

    @BusHandler
    public void onBorderLerpSize(ClientboundSetBorderLerpSizePacket packet) {
        borderState.setOldSize(packet.getOldSize());
        borderState.setNewSize(packet.getNewSize());
        borderState.setLerpTime(packet.getLerpTime());
    }

    @BusHandler
    public void onBorderSize(ClientboundSetBorderSizePacket packet) {
        borderState.setOldSize(borderState.getNewSize());
        borderState.setNewSize(packet.getSize());
    }

    @BusHandler
    public void onBorderWarningTime(ClientboundSetBorderWarningDelayPacket packet) {
        borderState.setWarningTime(packet.getWarningDelay());
    }

    @BusHandler
    public void onBorderWarningBlocks(ClientboundSetBorderWarningDistancePacket packet) {
        borderState.setWarningBlocks(packet.getWarningBlocks());
    }

    //
    // Entity packets
    //

    @BusHandler
    public void onEntitySpawn(ClientboundAddEntityPacket packet) {
        EntityState entityState = new EntityState(packet.getEntityId(), packet.getUuid(), packet.getType(), packet.getData());

        entityState.setPosition(packet.getX(), packet.getY(), packet.getZ());
        entityState.setRotation(packet.getYaw(), packet.getPitch());
        entityState.setHeadRotation(packet.getHeadYaw());
        entityState.setMotion(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());

        entityTrackerState.addEntity(packet.getEntityId(), entityState);
    }

    @BusHandler
    public void onPlayerSpawn(ClientboundAddPlayerPacket packet) {
        PlayerState playerState = new PlayerState(packet.getEntityId(), packet.getUuid());

        playerState.setPosition(packet.getX(), packet.getY(), packet.getZ());
        playerState.setRotation(packet.getYaw(), packet.getPitch());

        entityTrackerState.addEntity(packet.getEntityId(), playerState);
    }

    @BusHandler
    public void onExperienceOrbSpawn(ClientboundAddExperienceOrbPacket packet) {
        ExperienceOrbState experienceOrbState = new ExperienceOrbState(packet.getEntityId(), packet.getExp());

        experienceOrbState.setPosition(packet.getX(), packet.getY(), packet.getZ());

        entityTrackerState.addEntity(packet.getEntityId(), experienceOrbState);
    }

    @BusHandler
    public void onEntityRemove(ClientboundRemoveEntitiesPacket packet) {
        for (int entityId : packet.getEntityIds()) {
            entityTrackerState.removeEntity(entityId);
        }
    }

    @BusHandler
    public void onEntityData(ClientboundSetEntityDataPacket packet) {
        EntityMetadataState state = packet.getEntityId() == loginData.entityId() ?
                selfMetaData : entityTrackerState.getEntity(packet.getEntityId()).getMetadata();

        for (var entry : packet.getMetadata()) {
            state.setMetadata(entry);
        }
    }

    @BusHandler
    public void onEntityMotion(ClientboundSetEntityMotionPacket packet) {
        if (loginData.entityId() == packet.getEntityId()) {
            double motionX = packet.getMotionX();
            double motionY = packet.getMotionY();
            double motionZ = packet.getMotionZ();
            botMovementManager.setMotion(motionX, motionY, motionZ);
            log.debug("Bot forced to motion: {} {} {}", motionX, motionY, motionZ);
        } else {
            EntityLikeState state = entityTrackerState.getEntity(packet.getEntityId());

            state.setMotion(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
        }
    }

    @BusHandler
    public void onEntityPos(ClientboundMoveEntityPosPacket packet) {
        if (packet.getEntityId() == loginData.entityId()) {
            log.info("Received entity position packet for bot, notify the developers!");
            return;
        }

        EntityLikeState state = entityTrackerState.getEntity(packet.getEntityId());

        state.addPosition(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ());
        state.setOnGround(packet.isOnGround());
    }

    @BusHandler
    public void onEntityRot(ClientboundMoveEntityRotPacket packet) {
        if (packet.getEntityId() == loginData.entityId()) {
            log.info("Received entity rotation packet for bot, notify the developers!");
            return;
        }

        EntityLikeState state = entityTrackerState.getEntity(packet.getEntityId());

        state.setRotation(packet.getYaw(), packet.getPitch());
        state.setOnGround(packet.isOnGround());
    }

    @BusHandler
    public void onEntityRot(ClientboundRotateHeadPacket packet) {
        if (packet.getEntityId() == loginData.entityId()) {
            log.info("Received entity rotation packet for bot, notify the developers!");
            return;
        }

        EntityLikeState state = entityTrackerState.getEntity(packet.getEntityId());

        state.setHeadRotation(packet.getHeadYaw());
    }

    @BusHandler
    public void onEntityPosRot(ClientboundMoveEntityPosRotPacket packet) {
        if (packet.getEntityId() == loginData.entityId()) {
            log.info("Received entity position rotation packet for bot, notify the developers!");
            return;
        }

        EntityLikeState state = entityTrackerState.getEntity(packet.getEntityId());

        state.addPosition(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ());
        state.setRotation(packet.getYaw(), packet.getPitch());
        state.setOnGround(packet.isOnGround());
    }

    @BusHandler
    public void onEntityTeleport(ClientboundTeleportEntityPacket packet) {
        if (packet.getEntityId() == loginData.entityId()) {
            log.info("Received entity teleport packet for bot, notify the developers!");
            return;
        }

        EntityLikeState state = entityTrackerState.getEntity(packet.getEntityId());

        state.setPosition(packet.getX(), packet.getY(), packet.getZ());
        state.setRotation(packet.getYaw(), packet.getPitch());
        state.setOnGround(packet.isOnGround());
    }

    @BusHandler
    public void onResourcePack(ClientboundResourcePackPacket packet) {
        // TODO: Implement resource pack
        connection.session().send(new ServerboundResourcePackPacket(ResourcePackStatus.DECLINED));
    }

    @BusHandler
    public void onLoginDisconnectPacket(ClientboundLoginDisconnectPacket packet) {
        log.error("Login failed: {}", toPlainText(packet.getReason()));
    }

    @BusHandler
    public void onDisconnectPacket(ClientboundDisconnectPacket packet) {
        log.error("Disconnected: {}", toPlainText(packet.getReason()));
    }

    public void onDisconnectEvent(DisconnectedEvent event) {
        String reason = toPlainText(event.getReason());
        Throwable cause = event.getCause();
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

        cause.printStackTrace();
    }

    private String toPlainText(Component component) {
        return serverWrecker.getMessageSerializer().serialize(component);
    }

    public ChunkSection readChunkSection(ByteBuf buf, MinecraftCodecHelper codec) throws IOException {
        if (biomesEntryBitsSize == -1) {
            throw new IllegalStateException("Biome entry bits size is not set");
        }

        int blockCount = buf.readShort();

        DataPalette chunkPalette = codec.readDataPalette(buf, PaletteType.CHUNK,
                serverWrecker.getGlobalBlockPalette().getBlockBitsPerEntry());
        DataPalette biomePalette = codec.readDataPalette(buf, PaletteType.BIOME,
                biomesEntryBitsSize);
        return new ChunkSection(blockCount, chunkPalette, biomePalette);
    }

    public LevelState getCurrentLevel() {
        if (currentDimension == null) {
            return null;
        }

        return levels.get(currentDimension.dimensionType());
    }

    public void tick() {
        ServerWreckerAPI.postEvent(new BotPreTickEvent(connection));

        if (borderState != null) {
            borderState.tick();
        }

        LevelState level = getCurrentLevel();
        if (level != null && botMovementManager != null
                && level.isChunkLoaded(botMovementManager.getBlockPos())) {
            botMovementManager.tick();
        }

        ServerWreckerAPI.postEvent(new BotPostTickEvent(connection));
    }
}
