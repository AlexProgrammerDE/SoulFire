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
import io.jsonwebtoken.SignatureAlgorithm;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import lombok.Getter;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.attack.AttackInitEvent;
import net.pistonmaster.serverwrecker.auth.AccountList;
import net.pistonmaster.serverwrecker.auth.AccountRegistry;
import net.pistonmaster.serverwrecker.auth.AccountSettings;
import net.pistonmaster.serverwrecker.builddata.BuildData;
import net.pistonmaster.serverwrecker.common.AttackState;
import net.pistonmaster.serverwrecker.common.OperationMode;
import net.pistonmaster.serverwrecker.data.ResourceData;
import net.pistonmaster.serverwrecker.data.TranslationMapper;
import net.pistonmaster.serverwrecker.grpc.RPCClient;
import net.pistonmaster.serverwrecker.grpc.RPCServer;
import net.pistonmaster.serverwrecker.gui.navigation.SettingsPanel;
import net.pistonmaster.serverwrecker.logging.SWLogAppender;
import net.pistonmaster.serverwrecker.logging.SWTerminalConsole;
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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class ServerWrecker {
    public static final Logger LOGGER = LoggerFactory.getLogger(ServerWrecker.class);
    public static final Path DATA_FOLDER = Path.of(System.getProperty("user.home"), ".serverwrecker");
    public static final PlainTextComponentSerializer PLAIN_MESSAGE_SERIALIZER;

    // Static, but the preloading happens in ResourceData since we don't wanna init any constructors
    static {
        PLAIN_MESSAGE_SERIALIZER = PlainTextComponentSerializer.builder().flattener(
                ComponentFlattener.basic().toBuilder()
                        .mapper(TranslatableComponent.class, new TranslationMapper(ResourceData.MOJANG_TRANSLATIONS)).build()
        ).build();
    }

    private final Gson gson = new Gson();
    private final Injector injector = new InjectorBuilder()
            .addDefaultHandlers("net.pistonmaster.serverwrecker")
            .create();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Map<String, String> serviceServerConfig = new HashMap<>();
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private final SWTerminalConsole terminalConsole;
    private final AccountRegistry accountRegistry = new AccountRegistry();
    private final ProxyRegistry proxyRegistry = new ProxyRegistry();
    private final SettingsManager settingsManager = new SettingsManager(
            BotSettings.class,
            DevSettings.class,
            AccountSettings.class,
            AccountList.class,
            ProxySettings.class,
            ProxyList.class
    );
    private final OperationMode operationMode;
    private final Path profilesFolder;
    private final Path pluginsFolder;
    private final boolean outdated;
    private final Map<Integer, AttackManager> attacks = Collections.synchronizedMap(new Int2ObjectArrayMap<>());
    private final RPCServer rpcServer;
    private boolean shutdown = false;

    public ServerWrecker(OperationMode operationMode, String host, int port) {
        this.operationMode = operationMode;
        this.profilesFolder = DATA_FOLDER.resolve("profiles");
        this.pluginsFolder = DATA_FOLDER.resolve("plugins");

        // Register into injector
        injector.register(ServerWrecker.class, this);

        // Init API
        ServerWreckerAPI.setServerWrecker(this);

        var logAppender = new SWLogAppender();
        logAppender.start();
        injector.register(SWLogAppender.class, logAppender);
        ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(logAppender);

        KeyGenerator keyGen;
        try {
            keyGen = KeyGenerator.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        var jwtKey = keyGen.generateKey();

        rpcServer = new RPCServer(port, injector, jwtKey);
        try {
            rpcServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        settingsManager.registerDuplex(AccountList.class, accountRegistry);
        settingsManager.registerDuplex(ProxyList.class, proxyRegistry);

        LOGGER.info("Starting ServerWrecker v{}...", BuildData.VERSION);

        var jwt = Jwts.builder()
                .setSubject("admin")
                .signWith(jwtKey, SignatureAlgorithm.HS256)
                .compact();

        var rpcClient = new RPCClient(host, port, jwt);
        injector.register(RPCClient.class, rpcClient);

        terminalConsole = injector.getSingleton(SWTerminalConsole.class);

        try {
            Files.createDirectories(DATA_FOLDER);
            Files.createDirectories(profilesFolder);
            Files.createDirectories(pluginsFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Override status packet, so we can support any version
        MinecraftCodec.CODEC.getCodec(ProtocolState.STATUS)
                .registerClientbound(new PacketDefinition<>(0x00,
                        SWClientboundStatusResponsePacket.class,
                        new MinecraftPacketSerializer<>(SWClientboundStatusResponsePacket::new)));

        // Init via
        var viaPath = DATA_FOLDER.resolve("ViaVersion");
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
            new SWViaRewind(DATA_FOLDER.resolve("ViaRewind")).init();
            new SWViaBackwards(DATA_FOLDER.resolve("ViaBackwards")).init();
            new SWViaAprilFools(DATA_FOLDER.resolve("ViaAprilFools")).init();
            new SWViaLegacy(DATA_FOLDER.resolve("ViaLegacy")).init();
            new SWViaBedrock(DATA_FOLDER.resolve("ViaBedrock")).init();
        });

        var manager = (ViaManagerImpl) Via.getManager();
        manager.init();

        manager.getPlatform().getConf().setCheckForUpdates(false);

        manager.onServerLoaded();

        var settingsPanel = injector.getIfAvailable(SettingsPanel.class);
        if (settingsPanel != null) {
            settingsPanel.registerVersions();
        }

        for (var addon : ServerWreckerAPI.getAddons()) {
            addon.onEnable(this);
        }

        initPlugins(pluginsFolder);

        LOGGER.info("Checking for updates...");
        outdated = checkForUpdates();

        LOGGER.info("Finished loading!");
    }

    private boolean checkForUpdates() {
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
                response = gson.fromJson(new InputStreamReader(stream), JsonObject.class);
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

    public void initConsole() {
        terminalConsole.setupStreams();
        threadPool.execute(terminalConsole::start);
    }

    private void initPlugins(Path pluginDir) {
        try {
            Files.createDirectories(pluginDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create plugin directory", e);
        }

        // create the plugin manager
        PluginManager pluginManager = new JarPluginManager(pluginDir);

        pluginManager.setSystemVersion(BuildData.VERSION);

        // start and load all plugins of application
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
    }

    @SuppressWarnings("UnstableApiUsage")
    public void setupLoggingAndVia(DevSettings devSettings) {
        Via.getManager().debugHandler().setEnabled(devSettings.viaDebug());
        setupLogging(devSettings);
    }

    public static void setupLogging(DevSettings devSettings) {
        var level = devSettings.coreDebug() ? Level.DEBUG : Level.INFO;
        var nettyLevel = devSettings.nettyDebug() ? Level.DEBUG : Level.INFO;
        var grpcLevel = devSettings.grpcDebug() ? Level.DEBUG : Level.INFO;
        Configurator.setRootLevel(level);
        Configurator.setLevel(LOGGER.getName(), level);
        Configurator.setLevel("org.pf4j", level);
        Configurator.setLevel("io.netty", nettyLevel);
        Configurator.setLevel("io.grpc", grpcLevel);
    }

    /**
     * Shuts down the proxy, kicking players with the specified reason.
     *
     * @param explicitExit whether the user explicitly shut down the proxy
     */
    public void shutdown(boolean explicitExit) {
        if (!shutdownInProgress.compareAndSet(false, true)) {
            return;
        }

        if (explicitExit) {
            LOGGER.info("Shutting down...");
        }

        // Shutdown the attacks if there is any
        stopAllAttacks().join();

        // Shutdown scheduled tasks
        threadPool.shutdown();

        // Shut down RPC
        try {
            rpcServer.stop();
        } catch (InterruptedException e) {
            LOGGER.error("Failed to stop RPC server", e);
        }

        // Since we manually removed the shutdown hook, we need to handle the shutdown ourselves.
        LogManager.shutdown(true, true);

        shutdown = true;

        if (explicitExit) {
            System.exit(0);
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
}
