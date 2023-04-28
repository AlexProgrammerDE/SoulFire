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
import com.github.steveice10.mc.protocol.data.game.ResourcePackStatus;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkBiomeData;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.github.steveice10.mc.protocol.data.game.chunk.DataPalette;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.PaletteType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.GlobalPos;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockChangeEntry;
import com.github.steveice10.mc.protocol.data.game.level.notify.RainStrengthValue;
import com.github.steveice10.mc.protocol.data.game.level.notify.RespawnScreenValue;
import com.github.steveice10.mc.protocol.data.game.level.notify.ThunderStrengthValue;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityMotionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerClosePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.border.*;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundResourcePackPacket;
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
import net.pistonmaster.serverwrecker.api.event.bot.ChatMessageReceiveEvent;
import net.pistonmaster.serverwrecker.common.EntityLocation;
import net.pistonmaster.serverwrecker.common.EntityMotion;
import net.pistonmaster.serverwrecker.common.SWOptions;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.bot.container.Container;
import net.pistonmaster.serverwrecker.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.serverwrecker.protocol.bot.model.*;
import net.pistonmaster.serverwrecker.protocol.bot.state.BorderState;
import net.pistonmaster.serverwrecker.protocol.bot.state.ChunkData;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import net.pistonmaster.serverwrecker.protocol.bot.state.WeatherState;
import net.pistonmaster.serverwrecker.protocol.tcp.ViaClientSession;
import net.pistonmaster.serverwrecker.util.BusHandler;
import org.cloudburstmc.math.vector.Vector3i;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@ToString
public final class SessionDataManager {
    private final SWOptions options;
    private final Logger log;
    private final ServerWrecker serverWrecker;
    private final ViaClientSession session;
    private final BotConnection connection;
    private final WeatherState weatherState = new WeatherState();
    private final Map<Integer, AtomicInteger> itemCoolDowns = new ConcurrentHashMap<>();
    private final Map<String, LevelState> levels = new ConcurrentHashMap<>();
    private final Int2ObjectMap<BiomeData> biomes = new Int2ObjectOpenHashMap<>();
    private BorderState borderState;
    private EntityLocation location;
    private EntityMotion motion;
    private float health = -1;
    private int food = -1;
    private float saturation = -1;
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
    private @Nullable PlayerInventoryContainer playerInventoryContainer;
    private @Nullable Container openContainer;
    private int biomesEntryBitsSize = -1;
    private @Nullable ChunkKey centerChunk;
    private boolean isDead = false;

    public SessionDataManager(BotConnection connection) {
        this.options = connection.options();
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
    public void onPlayerChat(ClientboundPlayerChatPacket packet) {
        Component message = packet.getUnsignedContent();
        if (message != null) {
            onChat(message);
            return;
        }

        onChat(Component.text(packet.getContent()));
    }

    @BusHandler
    public void onServerChat(ClientboundSystemChatPacket packet) {
        onChat(packet.getContent());
    }

    private void onChat(Component message) {
        ServerWreckerAPI.postEvent(new ChatMessageReceiveEvent(connection, message));
    }

    @BusHandler
    public void onPosition(ClientboundPlayerPositionPacket packet) {
        try {
            location = new EntityLocation(packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch());
            log.info("Position updated: {}", location);
        } catch (Exception e) {
            log.error("Error while logging position", e);
        }
    }

    @BusHandler
    public void onHealth(ClientboundSetHealthPacket packet) {
        try {
            this.health = packet.getHealth();
            this.food = packet.getFood();
            this.saturation = packet.getSaturation();

            if (health < 1) {
                this.isDead = true;
            }
        } catch (Exception e) {
            log.error("Error while logging health", e);
        }
    }

    @BusHandler
    public void onDeath(ClientboundPlayerCombatKillPacket packet) {
        this.isDead = true;
    }

    @BusHandler
    public void onJoin(ClientboundLoginPacket packet) {
        try {
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

                levels.put(name, new LevelState(name, id, dimension.get("element")));
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

            log.info("Joined server");
        } catch (Exception e) {
            log.error("Error while logging join", e);
        }
    }

    @BusHandler
    public void onRespawn(ClientboundRespawnPacket packet) {
        try {
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
        } catch (Exception e) {
            log.error("Error while logging join", e);
        }
    }

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
    public void onExperience(ClientboundSetExperiencePacket packet) {
        experienceData = new ExperienceData(packet.getExperience(), packet.getLevel(), packet.getTotalExperience());
    }

    @BusHandler
    public void onSetInventoryContent(ClientboundContainerSetContentPacket packet) {
        if (packet.getContainerId() == 0) {
            PlayerInventoryContainer inventoryContainer = this.playerInventoryContainer;
            if (inventoryContainer == null) {
                inventoryContainer = new PlayerInventoryContainer();
            }

            for (int i = 0; i < packet.getItems().length; i++) {
                inventoryContainer.setSlot(i, packet.getItems()[i]);
            }

            this.playerInventoryContainer = inventoryContainer;
        } else log.debug("Received inventory content for unknown container: {}", packet.getContainerId());
    }

    @BusHandler
    public void onSetInventorySlot(ClientboundContainerSetSlotPacket packet) {
        if (packet.getContainerId() == 0) {
            PlayerInventoryContainer inventoryContainer = this.playerInventoryContainer;
            if (inventoryContainer == null) {
                inventoryContainer = new PlayerInventoryContainer();
            }

            inventoryContainer.setSlot(packet.getSlot(), packet.getItem());

            this.playerInventoryContainer = inventoryContainer;
        } else log.debug("Received inventory slot for unknown container: {}", packet.getContainerId());
    }

    @BusHandler
    public void onCloseInventory(ClientboundContainerClosePacket packet) {
        openContainer = null;
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
                log.info("Entered credits {} (Repawning now)", packet.getValue());
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
        getCurrentLevel().getChunks().remove(new ChunkKey(packet.getX(), packet.getZ()));
    }

    @BusHandler
    public void onChunkData(ClientboundLevelChunkWithLightPacket packet) {
        ChunkKey key = new ChunkKey(packet.getX(), packet.getZ());
        byte[] data = packet.getChunkData();
        ByteBuf buf = Unpooled.wrappedBuffer(data);
        LevelState level = getCurrentLevel();
        MinecraftCodecHelper helper = session.getCodecHelper();

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
        ChunkData chunkData = level.getChunks().get(key);

        if (chunkData == null) {
            log.warn("Received section update for unknown chunk: {}", key);
            return;
        }

        for (BlockChangeEntry entry : packet.getEntries()) {
            Vector3i vector3i = entry.getPosition();
            int newId = entry.getBlock();

            System.out.println("Updating block at " + vector3i + " to " + newId);
            level.setBlock(vector3i, newId);
        }
    }

    @BusHandler
    public void onBlockUpdate(ClientboundBlockUpdatePacket packet) {
        LevelState level = getCurrentLevel();
        BlockChangeEntry entry = packet.getEntry();

        Vector3i vector3i = entry.getPosition();
        int newId = entry.getBlock();

        level.setBlock(vector3i, newId);
    }

    @BusHandler
    public void onBlockDestruction(ClientboundBlockDestructionPacket packet) {
        // Indicates the ten states of a block-breaking animation
    }

    @BusHandler
    public void onBlockChangedAck(ClientboundBlockChangedAckPacket packet) {
        // TODO: Implement block break
    }

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

    @BusHandler
    public void onChunkData(ClientboundChunksBiomesPacket packet) {
        LevelState level = getCurrentLevel();
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
    public void onEntityMotion(ClientboundSetEntityMotionPacket packet) {
        try {
            if (loginData.entityId() == packet.getEntityId()) {
                motion = new EntityMotion(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
                //log.info("Player moved with motion: {} {} {}", motionX, motionY, motionZ);
            } else {
                //log.debug("Entity {} moved with motion: {} {} {}", entityId, motionX, motionY, motionZ);
            }
        } catch (Exception e) {
            log.error("Error while logging entity motion", e);
        }
    }

    @BusHandler
    public void onResourcePack(ClientboundResourcePackPacket packet) {
        // TODO: Implement resource pack
        connection.session().send(new ServerboundResourcePackPacket(ResourcePackStatus.DECLINED));
    }

    @BusHandler
    public void onLoginDisconnectPacket(ClientboundLoginDisconnectPacket packet) {
        try {
            log.error("Login failed: {}", toPlainText(packet.getReason()));
        } catch (Exception e) {
            log.error("Error while logging login failed", e);
        }
    }

    @BusHandler
    public void onDisconnectPacket(ClientboundDisconnectPacket packet) {
        try {
            log.error("Disconnected: {}", toPlainText(packet.getReason()));
        } catch (Exception e) {
            log.error("Error while logging disconnect", e);
        }
    }

    public void onDisconnectEvent(DisconnectedEvent event) {
        String reason = toPlainText(event.getReason());
        Throwable cause = event.getCause();
        try {
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
        } catch (Exception e) {
            log.error("Error while logging disconnect", e);
        }
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

    private LevelState getCurrentLevel() {
        return levels.get(currentDimension.dimensionType());
    }
}
