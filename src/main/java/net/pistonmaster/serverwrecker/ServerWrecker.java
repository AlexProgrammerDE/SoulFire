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
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
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
import io.netty.channel.EventLoopGroup;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.pistonmaster.serverwrecker.api.Addon;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.state.AttackEndEvent;
import net.pistonmaster.serverwrecker.api.event.state.AttackStartEvent;
import net.pistonmaster.serverwrecker.auth.*;
import net.pistonmaster.serverwrecker.builddata.BuildData;
import net.pistonmaster.serverwrecker.common.AttackState;
import net.pistonmaster.serverwrecker.common.OperationMode;
import net.pistonmaster.serverwrecker.data.TranslationMapper;
import net.pistonmaster.serverwrecker.gui.navigation.SettingsPanel;
import net.pistonmaster.serverwrecker.logging.SWTerminalConsole;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.BotConnectionFactory;
import net.pistonmaster.serverwrecker.protocol.bot.block.GlobalBlockPalette;
import net.pistonmaster.serverwrecker.protocol.netty.ResolveUtil;
import net.pistonmaster.serverwrecker.protocol.netty.SWNettyHelper;
import net.pistonmaster.serverwrecker.protocol.packet.SWClientboundStatusResponsePacket;
import net.pistonmaster.serverwrecker.proxy.ProxyList;
import net.pistonmaster.serverwrecker.proxy.ProxyRegistry;
import net.pistonmaster.serverwrecker.proxy.ProxySettings;
import net.pistonmaster.serverwrecker.proxy.SWProxy;
import net.pistonmaster.serverwrecker.settings.BotSettings;
import net.pistonmaster.serverwrecker.settings.DevSettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsHolder;
import net.pistonmaster.serverwrecker.settings.lib.SettingsManager;
import net.pistonmaster.serverwrecker.viaversion.SWViaLoader;
import net.pistonmaster.serverwrecker.viaversion.platform.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
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
    private final List<BotConnection> botConnections = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
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
    @Setter
    private AttackState attackState = AttackState.STOPPED;
    private boolean shutdown = false;

    public ServerWrecker(OperationMode operationMode) {
        this.operationMode = operationMode;
        this.profilesFolder = DATA_FOLDER.resolve("profiles");
        this.pluginsFolder = DATA_FOLDER.resolve("plugins");

        // Register into injector
        injector.register(ServerWrecker.class, this);

        // Init API
        ServerWreckerAPI.setServerWrecker(this);

        setupLogging(Level.INFO);

        settingsManager.registerDuplex(AccountList.class, accountRegistry);
        settingsManager.registerDuplex(ProxyList.class, proxyRegistry);

        logger.info("Starting ServerWrecker v{}...", BuildData.VERSION);

        terminalConsole = injector.getSingleton(SWTerminalConsole.class);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

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

        logger.info("Finished loading!");
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

    @SuppressWarnings("UnstableApiUsage")
    public void start() {
        SettingsHolder settingsHolder = settingsManager.collectSettings();
        BotSettings botSettings = settingsHolder.get(BotSettings.class);
        DevSettings devSettings = settingsHolder.get(DevSettings.class);
        AccountSettings accountSettings = settingsHolder.get(AccountSettings.class);
        ProxySettings proxySettings = settingsHolder.get(ProxySettings.class);

        Via.getManager().debugHandler().setEnabled(devSettings.debug());
        setupLogging(devSettings.debug() ? Level.DEBUG : Level.INFO);

        this.attackState = AttackState.RUNNING;

        logger.info("Preparing bot attack at {}", botSettings.host());

        int botAmount = botSettings.amount(); // How many bots to connect
        int botsPerProxy = proxySettings.botsPerProxy(); // How many bots per proxy are allowed
        List<SWProxy> proxies = proxyRegistry.getProxies();
        int availableProxiesCount = proxies.size(); // How many proxies are available?
        int maxBots = botsPerProxy > 0 ? botsPerProxy * availableProxiesCount : botAmount; // How many bots can be used at max

        if (botAmount > maxBots) {
            logger.warn("You have specified {} bots, but only {} are available.", botAmount, maxBots);
            logger.warn("You need {} more proxies to run this amount of bots.", (botAmount - maxBots) / botsPerProxy);
            logger.warn("Continuing with {} bots.", maxBots);
            botAmount = maxBots;
        }

        List<JavaAccount> accounts = new ArrayList<>(accountRegistry.getAccounts());
        int availableAccounts = accounts.size();

        if (availableAccounts > 0 && botAmount > availableAccounts) {
            logger.warn("You have specified {} bots, but only {} accounts are available.", botAmount, availableAccounts);
            logger.warn("Continuing with {} bots.", availableAccounts);
            botAmount = availableAccounts;
        }

        if (accountSettings.shuffleAccounts()) {
            Collections.shuffle(accounts);
        }

        Map<SWProxy, Integer> proxyUseMap = new Object2IntOpenHashMap<>();
        for (SWProxy proxy : proxies) {
            proxyUseMap.put(proxy, 0);
        }

        EventLoopGroup resolveGroup = SWNettyHelper.createEventLoopGroup();
        InetSocketAddress targetAddress = ResolveUtil.resolveAddress(settingsHolder, resolveGroup);

        Queue<BotConnectionFactory> factories = new ArrayBlockingQueue<>(botAmount);
        for (int botId = 1; botId <= botAmount; botId++) {
            SWProxy proxyData = getProxy(botsPerProxy, proxyUseMap);

            JavaAccount javaAccount = getAccount(accountSettings, accounts, botId);
            int index = accounts.indexOf(javaAccount);
            if (index != -1) {
                accounts.remove(index); // Remove the account from the list, so it can't be used again
            }

            factories.add(createBotFactory(targetAddress, settingsHolder, javaAccount, proxyData));
        }

        if (availableProxiesCount == 0) {
            logger.info("Starting attack at {} with {} bots", botSettings.host(), factories.size());
        } else {
            logger.info("Starting attack at {} with {} bots and {} proxies", botSettings.host(), factories.size(), availableProxiesCount);
        }

        ServerWreckerAPI.postEvent(new AttackStartEvent());

        while (!factories.isEmpty()) {
            try {
                TimeUnit.MILLISECONDS.sleep(botSettings.joinDelayMs());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            if (attackState.isStopped()) {
                break;
            }

            if (attackState.isPaused()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }

            BotConnectionFactory factory = factories.poll();
            if (factory == null) {
                break;
            }

            threadPool.execute(() -> {
                try {
                    factory.connect().join();
                } catch (Exception e) {
                    logger.error("Error while connecting", e);
                }
            });
        }
    }

    private JavaAccount getAccount(AccountSettings accountSettings, List<JavaAccount> accounts, int botId) {
        if (accounts.isEmpty()) {
            return new JavaAccount(String.format(accountSettings.nameFormat(), botId));
        } else {
            return accounts.get(0);
        }
    }

    private SWProxy getProxy(int accountsPerProxy, Map<SWProxy, Integer> proxyUseMap) {
        if (proxyUseMap.isEmpty()) {
            return null; // No proxies available
        } else {
            SWProxy selectedProxy = proxyUseMap.entrySet().stream()
                    .filter(entry -> accountsPerProxy == -1 || entry.getValue() < accountsPerProxy)
                    .min(Comparator.comparingInt(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElseThrow(() -> new IllegalStateException("No proxies available!")); // Should never happen

            // Always present
            proxyUseMap.computeIfPresent(selectedProxy, (proxy, useCount) -> useCount + 1);

            return selectedProxy;
        }
    }

    private BotConnectionFactory createBotFactory(InetSocketAddress targetAddress, SettingsHolder settingsHolder, JavaAccount javaAccount, SWProxy proxyData) {
        // AuthData will be used internally instead of the MCProtocol data
        MinecraftProtocol protocol = new MinecraftProtocol(new GameProfile(javaAccount.profileId(), javaAccount.username()), javaAccount.authToken());

        // Make sure this options is set to false, otherwise it will cause issues with ViaVersion
        protocol.setUseDefaultListeners(false);

        return new BotConnectionFactory(
                this,
                targetAddress,
                settingsHolder,
                LoggerFactory.getLogger(javaAccount.username()),
                protocol,
                javaAccount,
                proxyData
        );
    }

    public void stop() {
        if (attackState.isStopped()) {
            return;
        }

        this.attackState = AttackState.STOPPED;
        botConnections.forEach(BotConnection::disconnect);
        botConnections.clear();
        ServerWreckerAPI.postEvent(new AttackEndEvent());
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
        stop();

        // Shutdown scheduled tasks
        scheduler.shutdown();
        threadPool.shutdown();

        // Since we manually removed the shutdown hook, we need to handle the shutdown ourselves.
        LogManager.shutdown(true, true);

        shutdown = true;

        if (explicitExit) {
            System.exit(0);
        }
    }

    public void setupLogging(Level level) {
        Configurator.setRootLevel(level);
        Configurator.setLevel(logger.getName(), level);
        Configurator.setLevel("io.netty", level);
        Configurator.setLevel("org.pf4j", level);
    }
}
