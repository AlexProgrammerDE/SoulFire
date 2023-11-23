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
package net.pistonmaster.serverwrecker;

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
import lombok.Getter;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.pistonmaster.serverwrecker.api.ServerExtension;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.attack.AttackInitEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.SettingsManagerInitEvent;
import net.pistonmaster.serverwrecker.auth.AccountList;
import net.pistonmaster.serverwrecker.auth.AccountRegistry;
import net.pistonmaster.serverwrecker.auth.AccountSettings;
import net.pistonmaster.serverwrecker.builddata.BuildData;
import net.pistonmaster.serverwrecker.command.ShutdownManager;
import net.pistonmaster.serverwrecker.common.AttackState;
import net.pistonmaster.serverwrecker.common.OperationMode;
import net.pistonmaster.serverwrecker.data.ResourceData;
import net.pistonmaster.serverwrecker.data.TranslationMapper;
import net.pistonmaster.serverwrecker.grpc.RPCServer;
import net.pistonmaster.serverwrecker.logging.SWLogAppender;
import net.pistonmaster.serverwrecker.plugins.*;
import net.pistonmaster.serverwrecker.protocol.packet.SWClientboundStatusResponsePacket;
import net.pistonmaster.serverwrecker.proxy.ProxyList;
import net.pistonmaster.serverwrecker.proxy.ProxyRegistry;
import net.pistonmaster.serverwrecker.proxy.ProxySettings;
import net.pistonmaster.serverwrecker.settings.BotSettings;
import net.pistonmaster.serverwrecker.settings.DevSettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsHolder;
import net.pistonmaster.serverwrecker.settings.lib.SettingsManager;
import net.pistonmaster.serverwrecker.util.VersionComparator;
import net.pistonmaster.serverwrecker.viaversion.SWViaLoader;
import net.pistonmaster.serverwrecker.viaversion.platform.*;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class ServerWreckerServer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ServerWreckerServer.class);
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
            .addDefaultHandlers("net.pistonmaster.serverwrecker")
            .create();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Map<String, String> serviceServerConfig = new HashMap<>();
    private final AccountRegistry accountRegistry = new AccountRegistry();
    private final ProxyRegistry proxyRegistry = new ProxyRegistry();
    private final SettingsManager settingsManager = new SettingsManager(List.of(
            BotSettings.class,
            DevSettings.class,
            AccountSettings.class,
            AccountList.class,
            ProxySettings.class,
            ProxyList.class
    ));
    private final OperationMode operationMode;
    private final boolean outdated;
    private final Map<Integer, AttackManager> attacks = Collections.synchronizedMap(new Int2ObjectArrayMap<>());
    private final RPCServer rpcServer;
    private final ShutdownManager shutdownManager = new ShutdownManager(this::shutdownHook);
    private final SecretKey jwtSecretKey;

    public ServerWreckerServer(OperationMode operationMode, String host, int port) {
        this.operationMode = operationMode;

        // Register into injector
        injector.register(ServerWreckerServer.class, this);

        // Init API
        ServerWreckerAPI.setServerWrecker(this);

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

        LOGGER.info("Starting ServerWrecker v{}...", BuildData.VERSION);

        // Override status packet, so we can support any version
        MinecraftCodec.CODEC.getCodec(ProtocolState.STATUS)
                .registerClientbound(new PacketDefinition<>(0x00,
                        SWClientboundStatusResponsePacket.class,
                        new MinecraftPacketSerializer<>(SWClientboundStatusResponsePacket::new)));

        // Init via
        var viaPath = ServerWreckerBootstrap.DATA_FOLDER.resolve("ViaVersion");
        var platform = new SWViaPlatform(viaPath);

        Via.init(ViaManagerImpl.builder()
                .platform(platform)
                .injector(platform.getInjector())
                .loader(new SWViaLoader())
                .build());

        platform.init();

        // for ViaLegacy
        Via.getManager().getProtocolManager().setMaxProtocolPathSize(Integer.MAX_VALUE);
        Via.getManager().getProtocolManager().setMaxPathDeltaIncrease(-1);
        ((ProtocolManagerImpl) Via.getManager().getProtocolManager()).refreshVersions();

        Via.getManager().addEnableListener(() -> {
            new SWViaRewind(ServerWreckerBootstrap.DATA_FOLDER.resolve("ViaRewind")).init();
            new SWViaBackwards(ServerWreckerBootstrap.DATA_FOLDER.resolve("ViaBackwards")).init();
            new SWViaAprilFools(ServerWreckerBootstrap.DATA_FOLDER.resolve("ViaAprilFools")).init();
            new SWViaLegacy(ServerWreckerBootstrap.DATA_FOLDER.resolve("ViaLegacy")).init();
            new SWViaBedrock(ServerWreckerBootstrap.DATA_FOLDER.resolve("ViaBedrock")).init();
        });

        var manager = (ViaManagerImpl) Via.getManager();
        manager.init();

        manager.getPlatform().getConf().setCheckForUpdates(false);

        manager.onServerLoaded();

        registerInternalServerExtensions();
        registerServerExtensions();

        for (var serverExtension : ServerWreckerAPI.getServerExtensions()) {
            serverExtension.onEnable(this);
        }

        ServerWreckerAPI.postEvent(new SettingsManagerInitEvent(settingsManager));

        LOGGER.info("Checking for updates...");
        outdated = checkForUpdates();

        LOGGER.info("Finished loading!");
    }

    private static void registerInternalServerExtensions() {
        var plugins = List.of(
                new BotTicker(), new ClientBrand(), new ClientSettings(),
                new AutoReconnect(), new AutoRegister(), new AutoRespawn(),
                new AutoTotem(), new AutoJump(), new AutoArmor(), new AutoEat(),
                new ChatMessageLogger(), new ServerListBypass());

        plugins.forEach(ServerWreckerAPI::registerServerExtension);
    }

    private static void registerServerExtensions() {
        ServerWreckerBootstrap.PLUGIN_MANAGER.getExtensions(ServerExtension.class)
                .forEach(ServerWreckerAPI::registerServerExtension);
    }

    private static boolean checkForUpdates() {
        if (Boolean.getBoolean("serverwrecker.disable-updates")) {
            LOGGER.info("Skipping update check because of system property");
            return false;
        }

        try {
            var url = URI.create("https://api.github.com/repos/AlexProgrammerDE/ServerWrecker/releases/latest").toURL();
            var connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "ServerWrecker");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != 200) {
                LOGGER.warn("Failed to check for updates: {}", connection.getResponseCode());
                return false;
            }

            JsonObject response;
            try (var stream = connection.getInputStream()) {
                response = GENERAL_GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
            }

            var latestVersion = response.get("tag_name").getAsString();
            if (VersionComparator.isNewer(BuildData.VERSION, latestVersion)) {
                LOGGER.warn("ServerWrecker is outdated! Current version: {}, latest version: {}", BuildData.VERSION, latestVersion);
                return true;
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to check for updates", e);
        }

        return false;
    }

    /**
     * Generates a JWT for the admin user.
     *
     * @return The JWT.
     */
    public String generateAdminJWT() {
        return Jwts.builder()
                .subject("admin")
                .signWith(jwtSecretKey, Jwts.SIG.HS256)
                .compact();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void setupLoggingAndVia(SettingsHolder settingsHolder) {
        Via.getManager().debugHandler().setEnabled(settingsHolder.get(DevSettings.VIA_DEBUG));
        ServerWreckerBootstrap.setupLogging(settingsHolder);
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
            LOGGER.error("Failed to stop RPC server", e);
        }
    }

    public int startAttack() {
        return startAttack(settingsManager.collectSettings());
    }

    public int startAttack(SettingsHolder settingsHolder) {
        var attackManager = injector.newInstance(AttackManager.class);
        ServerWreckerAPI.postEvent(new AttackInitEvent(attackManager));

        attacks.put(attackManager.getId(), attackManager);

        attackManager.start(settingsHolder);

        LOGGER.debug("Started attack with id {}", attackManager.getId());

        return attackManager.getId();
    }

    public void toggleAttackState(int id, boolean pause) {
        attacks.get(id).setAttackState(pause ? AttackState.PAUSED : AttackState.RUNNING);
    }

    public CompletableFuture<Void> stopAllAttacks() {
        return CompletableFuture.allOf(Set.copyOf(attacks.keySet()).stream()
                .map(this::stopAttack).toArray(CompletableFuture[]::new));
    }

    public CompletableFuture<Void> stopAttack(int id) {
        return attacks.remove(id).stop();
    }

    public void shutdown() {
        shutdownManager.shutdown(true);
    }
}
