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
package net.pistonmaster.soulfire.server.protocol.bot;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.UnexpectedEncryptionException;
import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.data.game.ResourcePackStatus;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.PaletteType;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.GlobalPos;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerSpawnInfo;
import com.github.steveice10.mc.protocol.data.game.entity.player.PositionElement;
import com.github.steveice10.mc.protocol.data.game.level.notify.LimitedCraftingValue;
import com.github.steveice10.mc.protocol.data.game.level.notify.RainStrengthValue;
import com.github.steveice10.mc.protocol.data.game.level.notify.RespawnScreenValue;
import com.github.steveice10.mc.protocol.data.game.level.notify.ThunderStrengthValue;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundResourcePackPushPacket;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundResourcePackPacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddExperienceOrbPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.border.*;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.*;
import lombok.Getter;
import lombok.ToString;
import net.kyori.adventure.text.Component;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.soulfire.server.SoulFireServer;
import net.pistonmaster.soulfire.server.api.event.bot.BotJoinedEvent;
import net.pistonmaster.soulfire.server.api.event.bot.BotPostTickEvent;
import net.pistonmaster.soulfire.server.api.event.bot.BotPreTickEvent;
import net.pistonmaster.soulfire.server.api.event.bot.ChatMessageReceiveEvent;
import net.pistonmaster.soulfire.server.data.EntityType;
import net.pistonmaster.soulfire.server.data.ResourceData;
import net.pistonmaster.soulfire.server.protocol.BotConnection;
import net.pistonmaster.soulfire.server.protocol.bot.container.InventoryManager;
import net.pistonmaster.soulfire.server.protocol.bot.container.SWItemStack;
import net.pistonmaster.soulfire.server.protocol.bot.container.WindowContainer;
import net.pistonmaster.soulfire.server.protocol.bot.model.*;
import net.pistonmaster.soulfire.server.protocol.bot.movement.ControlState;
import net.pistonmaster.soulfire.server.protocol.bot.state.*;
import net.pistonmaster.soulfire.server.protocol.bot.state.entity.ClientEntity;
import net.pistonmaster.soulfire.server.protocol.bot.state.entity.ExperienceOrbEntity;
import net.pistonmaster.soulfire.server.protocol.bot.state.entity.RawEntity;
import net.pistonmaster.soulfire.server.protocol.netty.ViaClientSession;
import net.pistonmaster.soulfire.server.settings.BotSettings;
import net.pistonmaster.soulfire.server.settings.lib.SettingsHolder;
import net.pistonmaster.soulfire.server.util.PrimitiveHelper;
import net.pistonmaster.soulfire.server.viaversion.SWVersionConstants;
import org.cloudburstmc.math.vector.Vector3d;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@ToString
public final class SessionDataManager {
    private final SettingsHolder settingsHolder;
    private final Logger log;
    private final SoulFireServer soulFireServer;
    private final ViaClientSession session;
    private final BotConnection connection;
    private final WeatherState weatherState = new WeatherState();
    private final PlayerListState playerListState = new PlayerListState();
    private final Int2IntMap itemCoolDowns = Int2IntMaps.synchronize(new Int2IntOpenHashMap());
    private final Map<String, LevelState> levels = new ConcurrentHashMap<>();
    private final Int2ObjectMap<BiomeData> biomes = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<MapDataState> mapDataStates = new Int2ObjectOpenHashMap<>();
    private final EntityTrackerState entityTrackerState = new EntityTrackerState();
    private final InventoryManager inventoryManager = new InventoryManager(this);
    private final BotActionManager botActionManager = new BotActionManager(this);
    private final ControlState controlState = new ControlState();
    private final TagsState tagsState = new TagsState();
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
    private DimensionData currentDimension;
    private int serverViewDistance = -1;
    private int serverSimulationDistance = -1;
    private @Nullable GlobalPos lastDeathPos;
    private @Nullable DifficultyData difficultyData;
    private @Nullable AbilitiesData abilitiesData;
    private @Nullable DefaultSpawnData defaultSpawnData;
    private @Nullable ExperienceData experienceData;
    private int biomesEntryBitsSize = -1;
    private @Nullable ChunkKey centerChunk;
    private boolean isDead = false;
    private boolean joinedWorld = false;

    public SessionDataManager(BotConnection connection) {
        this.settingsHolder = connection.settingsHolder();
        this.log = connection.logger();
        this.soulFireServer = connection.soulFireServer();
        this.session = connection.session();
        this.connection = connection;
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
    public void onRegistry(ClientboundRegistryDataPacket packet) {
        var registry = packet.getRegistry();
        CompoundTag dimensionRegistry = registry.get("minecraft:dimension_type");
        for (var type : dimensionRegistry.<ListTag>get("value").getValue()) {
            var dimension = (CompoundTag) type;
            var name = dimension.<StringTag>get("name").getValue();
            int id = dimension.<IntTag>get("id").getValue();

            levels.put(name, new LevelState(this, name, id, dimension.get("element")));
        }
        CompoundTag biomeRegistry = registry.get("minecraft:worldgen/biome");
        for (var type : biomeRegistry.<ListTag>get("value").getValue()) {
            var biome = (CompoundTag) type;
            var biomeData = new BiomeData(biome);

            biomes.put(biomeData.id(), biomeData);
        }
        biomesEntryBitsSize = ChunkData.log2RoundUp(biomes.size());
    }

    @EventHandler
    public void onJoin(ClientboundLoginPacket packet) {
        // Set data from the packet
        loginData = new LoginPacketData(
                packet.isHardcore(),
                packet.getWorldNames(),
                packet.getMaxPlayers()
        );

        enableRespawnScreen = packet.isEnableRespawnScreen();
        doLimitedCrafting = packet.isDoLimitedCrafting();
        serverViewDistance = packet.getViewDistance();
        serverSimulationDistance = packet.getSimulationDistance();

        processSpawnInfo(packet.getCommonPlayerSpawnInfo());

        // Init client entity
        inventoryManager.initPlayerInventory();

        clientEntity = new ClientEntity(packet.getEntityId(), this, controlState);
        clientEntity.showReducedDebug(packet.isReducedDebugInfo());
        entityTrackerState.addEntity(clientEntity);
    }

    private void processSpawnInfo(PlayerSpawnInfo spawnInfo) {
        currentDimension = new DimensionData(
                spawnInfo.getDimension(),
                spawnInfo.getWorldName(),
                spawnInfo.getHashedSeed(),
                spawnInfo.isDebug(),
                spawnInfo.isFlat()
        );
        gameMode = spawnInfo.getGameMode();
        previousGameMode = spawnInfo.getPreviousGamemode();
        lastDeathPos = spawnInfo.getLastDeathPos();
    }

    @EventHandler
    public void onPosition(ClientboundPlayerPositionPacket packet) {
        var relative = packet.getRelative();
        var x = relative.contains(PositionElement.X) ? clientEntity.x() + packet.getX() : packet.getX();
        var y = relative.contains(PositionElement.Y) ? clientEntity.y() + packet.getY() : packet.getY();
        var z = relative.contains(PositionElement.Z) ? clientEntity.z() + packet.getZ() : packet.getZ();
        var yaw = relative.contains(PositionElement.YAW) ? clientEntity.yaw() + packet.getYaw() : packet.getYaw();
        var pitch = relative.contains(PositionElement.PITCH) ? clientEntity.pitch() + packet.getPitch() : packet.getPitch();

        clientEntity.setPosition(x, y, z);
        clientEntity.setRotation(yaw, pitch);

        var position = clientEntity.blockPos();
        if (!joinedWorld) {
            joinedWorld = true;

            log.info("Joined server at position: X {} Y {} Z {}", position.getX(), position.getY(), position.getZ());

            connection.eventBus().call(new BotJoinedEvent(connection));
        } else {
            log.debug("Position updated: X {} Y {} Z {}", position.getX(), position.getY(), position.getZ());
        }

        session.send(new ServerboundAcceptTeleportationPacket(packet.getTeleportId()));
        session.send(new ServerboundMovePlayerPosRotPacket(false, x, y, z, yaw, pitch));
    }

    @EventHandler
    public void onLookAt(ClientboundPlayerLookAtPacket packet) {
        clientEntity.lookAt(packet.getOrigin(),
                Vector3d.from(packet.getX(), packet.getY(), packet.getZ()));

        // TODO: Implement entity look at
    }

    @EventHandler
    public void onRespawn(ClientboundRespawnPacket packet) {
        processSpawnInfo(packet.getCommonPlayerSpawnInfo());

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
            session.send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
        }
    }

    @EventHandler
    public void onServerPlayData(ClientboundServerDataPacket packet) {
        serverPlayData = new ServerPlayData(
                packet.getMotd(),
                packet.getIconBytes(),
                packet.isEnforcesSecureChat()
        );
    }

    @EventHandler
    public void onPluginMessage(ClientboundCustomPayloadPacket packet) {
        log.debug("Received plugin message on channel {}", packet.getChannel());
        switch (packet.getChannel()) {
            case "minecraft:register" -> log.debug("Received register packet for channels: {}",
                    String.join(", ", readChannels(packet)));
            case "minecraft:unregister" -> log.debug("Received unregister packet for channels; {}",
                    String.join(", ", readChannels(packet)));
            case "minecraft:brand" -> log.debug("Received server brand \"{}\"",
                    session.getCodecHelper().readString(Unpooled.wrappedBuffer(packet.getData())));
        }
    }

    //
    // Chat packets
    //

    @EventHandler
    public void onPlayerChat(ClientboundPlayerChatPacket packet) {
        var message = packet.getUnsignedContent();
        if (message != null) {
            onChat(message);
            return;
        }

        onChat(Component.text(packet.getContent()));
    }

    @EventHandler
    public void onServerChat(ClientboundSystemChatPacket packet) {
        onChat(packet.getContent());
    }

    @EventHandler
    public void onDisguisedChat(ClientboundDisguisedChatPacket packet) {
    }

    private void onChat(Component message) {
        connection.eventBus().call(new ChatMessageReceiveEvent(connection, message));
    }

    //
    // Player list packets
    //

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

    //
    // Player data packets
    //

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
        abilitiesData = new AbilitiesData(
                packet.isInvincible(),
                packet.isFlying(),
                packet.isCanFly(),
                packet.isCreative(),
                packet.getFlySpeed(),
                packet.getWalkSpeed()
        );

        var attributeState = clientEntity.attributeState();
        attributeState.setAttribute(new Attribute(AttributeType.Builtin.GENERIC_MOVEMENT_SPEED, abilitiesData.walkSpeed()));
        attributeState.setAttribute(new Attribute(AttributeType.Builtin.GENERIC_FLYING_SPEED, abilitiesData.flySpeed()));

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
        experienceData = new ExperienceData(packet.getExperience(), packet.getLevel(), packet.getTotalExperience());
    }

    @EventHandler
    public void onLevelTime(ClientboundSetTimePacket packet) {
        var level = getCurrentLevel();

        if (level == null) {
            log.warn("Received time update while not in a level");
            return;
        }

        level.worldAge(packet.getWorldAge());
        level.time(packet.getTime());
    }

    //
    // Inventory packets
    //

    @EventHandler
    public void onSetContainerContent(ClientboundContainerSetContentPacket packet) {
        inventoryManager.lastStateId(packet.getStateId());
        var container = inventoryManager.getContainer(packet.getContainerId());

        if (container == null) {
            log.warn("Received container content update for unknown container {}", packet.getContainerId());
            return;
        }

        for (var i = 0; i < packet.getItems().length; i++) {
            container.setSlot(i, SWItemStack.from(packet.getItems()[i]));
        }
    }

    @EventHandler
    public void onSetContainerSlot(ClientboundContainerSetSlotPacket packet) {
        inventoryManager.lastStateId(packet.getStateId());
        if (packet.getContainerId() == -1 && packet.getSlot() == -1) {
            inventoryManager.cursorItem(SWItemStack.from(packet.getItem()));
            return;
        }

        var container = inventoryManager.getContainer(packet.getContainerId());

        if (container == null) {
            log.warn("Received container slot update for unknown container {}", packet.getContainerId());
            return;
        }

        container.setSlot(packet.getSlot(), SWItemStack.from(packet.getItem()));
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
        var container = new WindowContainer(packet.getType(), packet.getTitle(), packet.getContainerId());
        inventoryManager.setContainer(packet.getContainerId(), container);
        inventoryManager.openContainer(container);
    }

    @EventHandler
    public void onOpenBookScreen(ClientboundOpenBookPacket packet) {
    }

    @EventHandler
    public void onOpenHorseScreen(ClientboundHorseScreenOpenPacket packet) {
    }

    @EventHandler
    public void onCloseContainer(ClientboundContainerClosePacket packet) {
        inventoryManager.openContainer(null);
    }

    @EventHandler
    public void onMapData(ClientboundMapItemDataPacket packet) {
        mapDataStates.computeIfAbsent(packet.getMapId(), k -> new MapDataState()).update(packet);
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
                session.send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN)); // Respawns the player
            }
            case DEMO_MESSAGE -> log.debug("Demo event: {}", packet.getValue());
            case ARROW_HIT_PLAYER -> log.debug("Arrow hit player");
            case RAIN_STRENGTH -> weatherState.rainStrength(((RainStrengthValue) packet.getValue()).getStrength());
            case THUNDER_STRENGTH ->
                    weatherState.thunderStrength(((ThunderStrengthValue) packet.getValue()).getStrength());
            case PUFFERFISH_STING_SOUND -> log.debug("Pufferfish sting sound");
            case AFFECTED_BY_ELDER_GUARDIAN -> log.debug("Affected by elder guardian");
            case ENABLE_RESPAWN_SCREEN ->
                    enableRespawnScreen = packet.getValue() == RespawnScreenValue.ENABLE_RESPAWN_SCREEN;
            case LIMITED_CRAFTING -> doLimitedCrafting = packet.getValue() == LimitedCraftingValue.LIMITED_CRAFTING;
        }
    }

    @EventHandler
    public void onSetCenterChunk(ClientboundSetChunkCacheCenterPacket packet) {
        centerChunk = new ChunkKey(packet.getChunkX(), packet.getChunkZ());
    }

    //
    // Chunk packets
    //

    @EventHandler
    public void onChunkData(ClientboundLevelChunkWithLightPacket packet) {
        var helper = session.getCodecHelper();
        var level = getCurrentLevel();

        if (level == null) {
            log.warn("Received section update while not in a level");
            return;
        }

        var data = packet.getChunkData();
        var buf = Unpooled.wrappedBuffer(data);

        var chunkData = level.chunks().getOrCreateChunk(packet.getX(), packet.getZ());

        try {
            for (var i = 0; i < chunkData.getSectionCount(); i++) {
                chunkData.setSection(i, readChunkSection(buf, helper));
            }
        } catch (IOException e) {
            log.error("Failed to read chunk section", e);
        }
    }

    @EventHandler
    public void onChunkData(ClientboundChunksBiomesPacket packet) {
        var level = getCurrentLevel();

        if (level == null) {
            log.warn("Received section update while not in a level");
            return;
        }

        var codec = session.getCodecHelper();

        for (var biomeData : packet.getChunkBiomeData()) {
            var chunkData = level.chunks().getChunk(biomeData.getX(), biomeData.getZ());

            if (chunkData == null) {
                log.warn("Received biome update for unknown chunk: {} {}", biomeData.getX(), biomeData.getZ());
                return;
            }

            var buf = Unpooled.wrappedBuffer(biomeData.getBuffer());
            try {
                for (var i = 0; chunkData.getSectionCount() > i; i++) {
                    var section = chunkData.getSection(i);
                    var biomePalette = codec.readDataPalette(buf, PaletteType.BIOME, biomesEntryBitsSize);
                    chunkData.setSection(i, new ChunkSection(section.getBlockCount(), section.getChunkData(), biomePalette));
                }
            } catch (IOException e) {
                log.error("Failed to read chunk section", e);
            }
        }
    }

    @EventHandler
    public void onChunkForget(ClientboundForgetLevelChunkPacket packet) {
        var level = getCurrentLevel();

        if (level == null) {
            log.warn("Received section update while not in a level");
            return;
        }

        level.chunks().removeChunk(packet.getX(), packet.getZ());
    }

    //
    // Block packets
    //

    @EventHandler
    public void onSectionBlockUpdate(ClientboundSectionBlocksUpdatePacket packet) {
        var level = getCurrentLevel();

        if (level == null) {
            log.warn("Received section update while not in a level");
            return;
        }

        var chunkData = level.chunks().getChunk(packet.getChunkX(), packet.getChunkZ());

        if (chunkData == null) {
            log.warn("Received section blocks update for unknown chunk: {} {}", packet.getChunkX(), packet.getChunkZ());
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
        var level = getCurrentLevel();

        if (level == null) {
            log.warn("Received section update while not in a level");
            return;
        }

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

    //
    // World border packets
    //

    @EventHandler
    public void onBorderInit(ClientboundInitializeBorderPacket packet) {
        borderState = new BorderState(packet.getNewCenterX(), packet.getNewCenterZ(), packet.getOldSize(), packet.getNewSize(),
                packet.getLerpTime(), packet.getNewAbsoluteMaxSize(), packet.getWarningBlocks(), packet.getWarningTime());
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

    //
    // Entity packets
    //

    @EventHandler
    public void onEntitySpawn(ClientboundAddEntityPacket packet) {
        var entityState = new RawEntity(packet.getEntityId(), packet.getUuid(), EntityType.getById(packet.getType().ordinal()), packet.getData());

        entityState.setPosition(packet.getX(), packet.getY(), packet.getZ());
        entityState.setRotation(packet.getYaw(), packet.getPitch());
        entityState.setHeadRotation(packet.getHeadYaw());
        entityState.setMotion(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());

        entityTrackerState.addEntity(entityState);
    }

    @EventHandler
    public void onExperienceOrbSpawn(ClientboundAddExperienceOrbPacket packet) {
        var experienceOrbState = new ExperienceOrbEntity(packet.getEntityId(), packet.getExp());

        experienceOrbState.setPosition(packet.getX(), packet.getY(), packet.getZ());

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
            state.attributeState().setAttribute(entry);
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

        state.effectState().updateEffect(
                packet.getEffect(),
                packet.getAmplifier(),
                packet.getDuration(),
                packet.isAmbient(),
                packet.isShowParticles()
        );
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
            log.warn("Received entity position rotation packet for unknown entity {}", packet.getEntityId());
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
            sendPacket(new ServerboundResourcePackPacket(
                    packet.getId(),
                    ResourcePackStatus.INVALID_URL
            ));
            return;
        }

        var version = settingsHolder.get(BotSettings.PROTOCOL_VERSION, ProtocolVersion::getClosest);
        if (SWVersionConstants.isBedrock(version)) {
            sendPacket(new ServerboundResourcePackPacket(
                    packet.getId(),
                    ResourcePackStatus.DECLINED
            ));
            return;
        }

        sendPacket(new ServerboundResourcePackPacket(
                packet.getId(),
                ResourcePackStatus.ACCEPTED
        ));
        sendPacket(new ServerboundResourcePackPacket(
                packet.getId(),
                ResourcePackStatus.DOWNLOADED
        ));
        sendPacket(new ServerboundResourcePackPacket(
                packet.getId(),
                ResourcePackStatus.SUCCESSFULLY_LOADED
        ));
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
        log.error("Login failed with reason \"{}\"", toPlainText(packet.getReason()));
    }

    @EventHandler
    public void onDisconnectPacket(ClientboundDisconnectPacket packet) {
        log.info("Disconnected with reason \"{}\"", toPlainText(packet.getReason()));
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

    public ChunkSection readChunkSection(ByteBuf buf, MinecraftCodecHelper codec) throws IOException {
        if (biomesEntryBitsSize == -1) {
            throw new IllegalStateException("Biome entry bits size is not set");
        }

        int blockCount = buf.readShort();

        var chunkPalette = codec.readDataPalette(buf, PaletteType.CHUNK,
                ResourceData.GLOBAL_BLOCK_PALETTE.blockBitsPerEntry());
        var biomePalette = codec.readDataPalette(buf, PaletteType.BIOME,
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
        connection.eventBus().call(new BotPreTickEvent(connection));

        // Tick border changes
        if (borderState != null) {
            borderState.tick();
        }

        // Tick item cooldowns
        tickCoolDowns();

        // Tick entities
        entityTrackerState.tick();

        connection.eventBus().call(new BotPostTickEvent(connection));
    }

    private void tickCoolDowns() {
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

    public void sendPacket(Packet packet) {
        session.send(packet);
    }
}
