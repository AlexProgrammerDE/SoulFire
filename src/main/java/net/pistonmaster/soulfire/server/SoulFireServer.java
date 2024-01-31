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
package net.pistonmaster.soulfire.server;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.github.steveice10.mc.protocol.codec.MinecraftPacketSerializer;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.packetlib.codec.PacketDefinition;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.protocol.ProtocolManagerImpl;
import io.jsonwebtoken.Jwts;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.pistonmaster.soulfire.SoulFireBootstrap;
import net.pistonmaster.soulfire.builddata.BuildData;
import net.pistonmaster.soulfire.server.api.AttackState;
import net.pistonmaster.soulfire.server.api.ServerExtension;
import net.pistonmaster.soulfire.server.api.SoulFireAPI;
import net.pistonmaster.soulfire.server.api.event.attack.AttackInitEvent;
import net.pistonmaster.soulfire.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.soulfire.server.data.ResourceData;
import net.pistonmaster.soulfire.server.data.TranslationMapper;
import net.pistonmaster.soulfire.server.grpc.RPCServer;
import net.pistonmaster.soulfire.server.plugins.*;
import net.pistonmaster.soulfire.server.protocol.packet.SWClientboundStatusResponsePacket;
import net.pistonmaster.soulfire.server.settings.AccountSettings;
import net.pistonmaster.soulfire.server.settings.BotSettings;
import net.pistonmaster.soulfire.server.settings.DevSettings;
import net.pistonmaster.soulfire.server.settings.ProxySettings;
import net.pistonmaster.soulfire.server.settings.lib.ServerSettingsRegistry;
import net.pistonmaster.soulfire.server.settings.lib.SettingsHolder;
import net.pistonmaster.soulfire.server.util.SWLogAppender;
import net.pistonmaster.soulfire.server.util.VersionComparator;
import net.pistonmaster.soulfire.server.viaversion.SWViaLoader;
import net.pistonmaster.soulfire.server.viaversion.platform.*;
import net.pistonmaster.soulfire.util.SWPathConstants;
import net.pistonmaster.soulfire.util.ShutdownManager;
import org.apache.logging.log4j.LogManager;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Getter
public class SoulFireServer {
    public static final PlainTextComponentSerializer PLAIN_MESSAGE_SERIALIZER;
    public static final Gson GENERAL_GSON = new Gson();

    // Static, but the preloading happens in ResourceData since we don't wanna init any constructors
    static {
        PLAIN_MESSAGE_SERIALIZER = PlainTextComponentSerializer.builder().flattener(
                ComponentFlattener.basic()
                        .toBuilder()
                        .mapper(TranslatableComponent.class, new TranslationMapper(ResourceData.MOJANG_TRANSLATIONS))
                        .build()
        ).build();
    }

    private final Injector injector = new InjectorBuilder()
            .addDefaultHandlers("net.pistonmaster.soulfire")
            .create();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Map<String, String> serviceServerConfig = new HashMap<>();
    private final ServerSettingsRegistry settingsRegistry = new ServerSettingsRegistry()
            .addClass(BotSettings.class, "Bot Settings", true)
            .addClass(DevSettings.class, "Dev Settings", true)
            .addClass(AccountSettings.class, "Account Settings", true)
            .addClass(ProxySettings.class, "Proxy Settings", true);
    private final boolean outdated;
    private final Int2ObjectMap<AttackManager> attacks = Int2ObjectMaps.synchronize(new Int2ObjectArrayMap<>());
    private final RPCServer rpcServer;
    private final ShutdownManager shutdownManager = new ShutdownManager(this::shutdownHook);
    private final SecretKey jwtSecretKey;

    public SoulFireServer(String host, int port) {
        // Register into injector
        injector.register(SoulFireServer.class, this);

        // Init API
        SoulFireAPI.setSoulFire(this);

        injector.register(ShutdownManager.class, shutdownManager);

        var logAppender = new SWLogAppender();
        logAppender.start();
        injector.register(SWLogAppender.class, logAppender);
        ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(logAppender);

        try {
            var keyGen = KeyGenerator.getInstance("HmacSHA256");
            this.jwtSecretKey = keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        rpcServer = new RPCServer(host, port, injector, jwtSecretKey);
        try {
            rpcServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("Starting SoulFire v{}...", BuildData.VERSION);

        // Override status packet, so we can support any version
        MinecraftCodec.CODEC.getCodec(ProtocolState.STATUS)
                .registerClientbound(new PacketDefinition<>(0x00,
                        SWClientboundStatusResponsePacket.class,
                        new MinecraftPacketSerializer<>(SWClientboundStatusResponsePacket::new)));

        // Init via
        var viaPath = SWPathConstants.CONFIG_FOLDER.resolve("ViaVersion");
        var platform = new SWViaPlatform(viaPath);

        Via.init(ViaManagerImpl.builder()
                .platform(platform)
                .injector(platform.injector())
                .loader(new SWViaLoader())
                .build());

        platform.init();

        // for ViaLegacy
        Via.getManager().getProtocolManager().setMaxProtocolPathSize(Integer.MAX_VALUE);
        Via.getManager().getProtocolManager().setMaxPathDeltaIncrease(-1);
        ((ProtocolManagerImpl) Via.getManager().getProtocolManager()).refreshVersions();

        Via.getManager().addEnableListener(() -> {
            new SWViaRewind(SWPathConstants.CONFIG_FOLDER.resolve("ViaRewind")).init();
            new SWViaBackwards(SWPathConstants.CONFIG_FOLDER.resolve("ViaBackwards")).init();
            new SWViaAprilFools(SWPathConstants.CONFIG_FOLDER.resolve("ViaAprilFools")).init();
            new SWViaLegacy(SWPathConstants.CONFIG_FOLDER.resolve("ViaLegacy")).init();
            new SWViaBedrock(SWPathConstants.CONFIG_FOLDER.resolve("ViaBedrock")).init();
        });

        var manager = (ViaManagerImpl) Via.getManager();
        manager.init();

        manager.getPlatform().getConf().setCheckForUpdates(false);

        manager.onServerLoaded();

        registerInternalServerExtensions();
        registerServerExtensions();

        for (var serverExtension : SoulFireAPI.getServerExtensions()) {
            serverExtension.onEnable(this);
        }

        SoulFireAPI.postEvent(new SettingsRegistryInitEvent(settingsRegistry));

        log.info("Checking for updates...");
        outdated = checkForUpdates();

        log.info("Finished loading!");
    }

    private static void registerInternalServerExtensions() {
        var plugins = List.of(
                new BotTicker(), new ClientBrand(), new ClientSettings(),
                new AutoReconnect(), new AutoRegister(), new AutoRespawn(),
                new AutoTotem(), new AutoJump(), new AutoArmor(), new AutoEat(),
                new ChatMessageLogger(), new ServerListBypass(),
                new FakeVirtualHost(), // Needs to be before ModLoaderSupport to not break it
                new ModLoaderSupport(), // Needs to be before ForwardingBypass to not break it
                new ForwardingBypass()
        );

        plugins.forEach(SoulFireAPI::registerServerExtension);
    }

    private static void registerServerExtensions() {
        SoulFireBootstrap.PLUGIN_MANAGER.getExtensions(ServerExtension.class)
                .forEach(SoulFireAPI::registerServerExtension);
    }

    private static boolean checkForUpdates() {
        if (Boolean.getBoolean("soulfire.disable-updates")) {
            log.info("Skipping update check because of system property");
            return false;
        }

        try {
            var url = URI.create("https://api.github.com/repos/AlexProgrammerDE/SoulFire/releases/latest").toURL();
            var connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "SoulFire");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != 200) {
                log.warn("Failed to check for updates: {}", connection.getResponseCode());
                return false;
            }

            JsonObject response;
            try (var stream = connection.getInputStream()) {
                response = GENERAL_GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
            }

            var latestVersion = response.get("tag_name").getAsString();
            if (VersionComparator.isNewer(BuildData.VERSION, latestVersion)) {
                log.warn("SoulFire is outdated! Current version: {}, latest version: {}", BuildData.VERSION, latestVersion);
                return true;
            }
        } catch (IOException e) {
            log.warn("Failed to check for updates", e);
        }

        return false;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void setupLoggingAndVia(SettingsHolder settingsHolder) {
        Via.getManager().debugHandler().setEnabled(settingsHolder.get(DevSettings.VIA_DEBUG));
        SoulFireBootstrap.setupLogging(settingsHolder);
    }

    public String generateAdminJWT() {
        return generateJWT("admin");
    }

    public String generateLocalCliJWT() {
        return generateJWT("local-cli");
    }

    private String generateJWT(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(Instant.now()))
                .signWith(jwtSecretKey, Jwts.SIG.HS256)
                .compact();
    }

    private void shutdownHook() {
        // Shutdown the attacks if there is any
        stopAllAttacks().join();

        // Shutdown scheduled tasks
        threadPool.shutdown();

        // Shut down RPC
        try {
            rpcServer.shutdown();
        } catch (InterruptedException e) {
            log.error("Failed to stop RPC server", e);
        }
    }

    public int startAttack(SettingsHolder settingsHolder) {
        var attackManager = injector.newInstance(AttackManager.class);
        SoulFireAPI.postEvent(new AttackInitEvent(attackManager));

        attacks.put(attackManager.id(), attackManager);

        attackManager.start(settingsHolder);

        log.debug("Started attack with id {}", attackManager.id());

        return attackManager.id();
    }

    public void toggleAttackState(int id, boolean pause) {
        attacks.get(id).attackState(pause ? AttackState.PAUSED : AttackState.RUNNING);
    }

    public CompletableFuture<?> stopAllAttacks() {
        return CompletableFuture.allOf(Set.copyOf(attacks.keySet()).stream()
                .map(this::stopAttack).toArray(CompletableFuture[]::new));
    }

    public CompletableFuture<?> stopAttack(int id) {
        return attacks.remove(id).stop();
    }

    public void shutdown() {
        shutdownManager.shutdownSoftware(true);
    }
}
