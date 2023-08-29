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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.protocol.ProtocolManagerImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Getter;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.pistonmaster.serverwrecker.api.Addon;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.auth.AccountList;
import net.pistonmaster.serverwrecker.auth.AccountRegistry;
import net.pistonmaster.serverwrecker.auth.AccountSettings;
import net.pistonmaster.serverwrecker.builddata.BuildData;
import net.pistonmaster.serverwrecker.common.OperationMode;
import net.pistonmaster.serverwrecker.data.TranslationMapper;
import net.pistonmaster.serverwrecker.grpc.RPCClient;
import net.pistonmaster.serverwrecker.grpc.RPCServer;
import net.pistonmaster.serverwrecker.gui.navigation.SettingsPanel;
import net.pistonmaster.serverwrecker.logging.LogAppender;
import net.pistonmaster.serverwrecker.logging.SWTerminalConsole;
import net.pistonmaster.serverwrecker.protocol.bot.block.GlobalBlockPalette;
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
import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class ServerWrecker {
    public static final Path DATA_FOLDER = Path.of(System.getProperty("user.home"), ".serverwrecker");

    static {
        MinecraftCodec.CODEC.getCodec(ProtocolState.STATUS)
                .registerClientbound(new PacketDefinition<>(0x00,
                        SWClientboundStatusResponsePacket.class,
                        new MinecraftPacketSerializer<>(SWClientboundStatusResponsePacket::new)));
    }

    private final Logger logger = LoggerFactory.getLogger("ServerWrecker");
    private final Injector injector = new InjectorBuilder()
            .addDefaultHandlers("net.pistonmaster.serverwrecker")
            .create();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Map<String, String> serviceServerConfig = new HashMap<>();
    private final Gson gson = new Gson();
    private final Map<String, String> mojangTranslations = new HashMap<>();
    private final GlobalBlockPalette globalBlockPalette;
    private final PlainTextComponentSerializer messageSerializer;
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private final SWTerminalConsole terminalConsole;
    private final AccountRegistry accountRegistry = new AccountRegistry(this);
    private final ProxyRegistry proxyRegistry = new ProxyRegistry(this);
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
    private final Set<AttackManager> attacks = Collections.synchronizedSet(new HashSet<>());
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

        setupLogging(Level.INFO);

        LogAppender logAppender = new LogAppender();
        logAppender.start();
        injector.register(LogAppender.class, logAppender);
        ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(logAppender);

        KeyGenerator keyGen;
        try {
            keyGen = KeyGenerator.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        SecretKey jwtKey = keyGen.generateKey();

        rpcServer = new RPCServer(port, injector, jwtKey);
        try {
            rpcServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        settingsManager.registerDuplex(AccountList.class, accountRegistry);
        settingsManager.registerDuplex(ProxyList.class, proxyRegistry);

        logger.info("Starting ServerWrecker v{}...", BuildData.VERSION);

        String jwt = Jwts.builder()
                .setSubject("admin")
                .signWith(jwtKey, SignatureAlgorithm.HS256)
                .compact();

        RPCClient rpcClient = new RPCClient(host, port, jwt);
        injector.register(RPCClient.class, rpcClient);

        terminalConsole = injector.getSingleton(SWTerminalConsole.class);

        try {
            Files.createDirectories(DATA_FOLDER);
            Files.createDirectories(profilesFolder);
            Files.createDirectories(pluginsFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JsonObject translations;
        try (InputStream stream = ServerWrecker.class.getClassLoader().getResourceAsStream("minecraft/en_us.json")) {
            Objects.requireNonNull(stream, "en_us.json not found");
            translations = gson.fromJson(new InputStreamReader(stream), JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Map.Entry<String, JsonElement> translationEntry : translations.entrySet()) {
            mojangTranslations.put(translationEntry.getKey(), translationEntry.getValue().getAsString());
        }

        this.messageSerializer = PlainTextComponentSerializer.builder().flattener(
                ComponentFlattener.basic().toBuilder()
                        .mapper(TranslatableComponent.class, new TranslationMapper(this, logger)).build()
        ).build();

        logger.info("Loaded {} mojang translations", mojangTranslations.size());

        // Load block states
        JsonObject blocks;
        try (InputStream stream = ServerWrecker.class.getClassLoader().getResourceAsStream("minecraft/blocks.json")) {
            Objects.requireNonNull(stream, "blocks.json not found");
            blocks = gson.fromJson(new InputStreamReader(stream), JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Create global palette
        Map<Integer, String> stateMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> blockEntry : blocks.entrySet()) {
            for (JsonElement state : blockEntry.getValue().getAsJsonObject().get("states").getAsJsonArray()) {
                stateMap.put(state.getAsJsonObject().get("id").getAsInt(), blockEntry.getKey());
            }
        }

        globalBlockPalette = new GlobalBlockPalette(stateMap.size());
        for (Map.Entry<Integer, String> entry : stateMap.entrySet()) {
            globalBlockPalette.add(entry.getKey(), entry.getValue());
        }

        logger.info("Loaded {} block states", stateMap.size());

        // Init via
        Path viaPath = DATA_FOLDER.resolve("ViaVersion");
        SWViaPlatform platform = new SWViaPlatform(viaPath);

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

        ViaManagerImpl manager = (ViaManagerImpl) Via.getManager();
        manager.init();

        manager.getPlatform().getConf().setCheckForUpdates(false);

        manager.onServerLoaded();

        SettingsPanel settingsPanel = injector.getIfAvailable(SettingsPanel.class);
        if (settingsPanel != null) {
            settingsPanel.registerVersions();
        }

        for (Addon addon : ServerWreckerAPI.getAddons()) {
            addon.onEnable(this);
        }

        initPlugins(pluginsFolder);

        logger.info("Checking for updates...");
        outdated = checkForUpdates();

        logger.info("Finished loading!");
    }

    private boolean checkForUpdates() {
        try {
            URL url = new URL("https://api.github.com/repos/AlexProgrammerDE/ServerWrecker/releases/latest");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "ServerWrecker");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != 200) {
                logger.warn("Failed to check for updates: {}", connection.getResponseCode());
                return false;
            }

            JsonObject response;
            try (InputStream stream = connection.getInputStream()) {
                response = gson.fromJson(new InputStreamReader(stream), JsonObject.class);
            }

            String latestVersion = response.get("tag_name").getAsString();
            if (VersionComparator.isNewer(BuildData.VERSION, latestVersion)) {
                logger.warn("ServerWrecker is outdated! Current version: {}, latest version: {}", BuildData.VERSION, latestVersion);
                return true;
            }
        } catch (IOException e) {
            logger.warn("Failed to check for updates", e);
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
            e.printStackTrace();
        }

        // create the plugin manager
        PluginManager pluginManager = new JarPluginManager(pluginDir);

        pluginManager.setSystemVersion(BuildData.VERSION);

        // start and load all plugins of application
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
    }

    public void setupLogging(Level level) {
        Configurator.setRootLevel(level);
        Configurator.setLevel(logger.getName(), level);
        Configurator.setLevel("io.netty", level);
        Configurator.setLevel("org.pf4j", level);
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
            logger.info("Shutting down...");
        }

        // Shutdown the attack if there is any
        stopAllAttacks().join();

        // Shutdown scheduled tasks
        threadPool.shutdown();

        // Shut down RPC
        try {
            rpcServer.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Since we manually removed the shutdown hook, we need to handle the shutdown ourselves.
        LogManager.shutdown(true, true);

        shutdown = true;

        if (explicitExit) {
            System.exit(0);
        }
    }

    public CompletableFuture<Void> stopAllAttacks() {
        synchronized (attacks) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (AttackManager attackManager : attacks) {
                futures.add(attackManager.stop());
            }

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        }
    }

    public void startAttack() {
        startAttack(settingsManager.collectSettings());
    }

    public void startAttack(SettingsHolder settingsHolder) {
        AttackManager attackManager = injector.newInstance(AttackManager.class);

        attackManager.start(settingsHolder);

        attacks.add(attackManager);
    }
}
