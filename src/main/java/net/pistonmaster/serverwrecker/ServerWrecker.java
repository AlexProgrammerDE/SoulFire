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
import ch.qos.logback.classic.Level;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.protocol.ProtocolManagerImpl;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.pistonmaster.serverwrecker.addons.*;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.state.AttackEndEvent;
import net.pistonmaster.serverwrecker.api.event.state.AttackStartEvent;
import net.pistonmaster.serverwrecker.builddata.BuildData;
import net.pistonmaster.serverwrecker.common.*;
import net.pistonmaster.serverwrecker.gui.navigation.SettingsPanel;
import net.pistonmaster.serverwrecker.logging.LogUtil;
import net.pistonmaster.serverwrecker.mojangdata.TranslationMapper;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.BotConnectionFactory;
import net.pistonmaster.serverwrecker.protocol.OfflineAuthenticationService;
import net.pistonmaster.serverwrecker.protocol.bot.block.GlobalBlockPalette;
import net.pistonmaster.serverwrecker.viaversion.SWViaLoader;
import net.pistonmaster.serverwrecker.viaversion.platform.*;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class ServerWrecker {
    private final Logger logger = LoggerFactory.getLogger("ServerWrecker");
    private final Injector injector = new InjectorBuilder()
            .addDefaultHandlers("net.pistonmaster.serverwrecker")
            .create();
    private final Set<InternalAddon> addons = Set.of(
            new BotsTicker(),
            new AutoReconnect(), new AutoRegister(), new AutoRespawn(),
            new ChatMessageLogger(), new ServerListBypass());
    private final List<BotConnection> botConnections = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private final List<BotProxy> passWordProxies = new ArrayList<>();
    private final Map<String, String> serviceServerConfig = new HashMap<>();
    private final Gson gson = new Gson();
    private final Map<String, String> mojangTranslations = new HashMap<>();
    private final GlobalBlockPalette globalBlockPalette;
    private final PlainTextComponentSerializer messageSerializer;
    @Setter
    private AttackState attackState = AttackState.STOPPED;
    @Setter
    private List<String> accounts;

    public ServerWrecker(Path dataFolder) {
        // Register into injector
        injector.register(ServerWrecker.class, this);

        // Init API
        ServerWreckerAPI.setServerWrecker(this);

        setupLogging(Level.INFO);

        logger.info("Starting ServerWrecker v{}...", BuildData.VERSION);

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
        Path viaPath = dataFolder.resolve("ViaVersion");
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
            new SWViaRewind(dataFolder.resolve("ViaRewind")).init();
            new SWViaBackwards(dataFolder.resolve("ViaBackwards")).init();
            new SWViaAprilFools(dataFolder.resolve("ViaAprilFools")).init();
            new SWViaLegacy(dataFolder.resolve("ViaLegacy")).init();
            new SWViaBedrock(dataFolder.resolve("ViaBedrock")).init();
        });

        ViaManagerImpl manager = (ViaManagerImpl) Via.getManager();
        manager.init();

        manager.getPlatform().getConf().setCheckForUpdates(false);

        manager.onServerLoaded();

        SettingsPanel settingsPanel = injector.getIfAvailable(SettingsPanel.class);
        if (settingsPanel != null) {
            settingsPanel.registerVersions();
        }

        for (InternalAddon addon : addons) {
            addon.init(this);
        }

        initPlugins(dataFolder.resolve("plugins"));

        logger.info("Finished loading!");
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
    public void start(SWOptions options) {
        Via.getManager().debugHandler().setEnabled(options.debug());
        setupLogging(options.debug() ? Level.DEBUG : Level.INFO);

        this.attackState = AttackState.RUNNING;

        List<BotProxy> proxyCache = passWordProxies.isEmpty() ? Collections.emptyList() : ImmutableList.copyOf(passWordProxies);
        Iterator<BotProxy> proxyIterator = proxyCache.listIterator();
        Map<BotProxy, AtomicInteger> proxyUseMap = new HashMap<>();
        List<BotConnectionFactory> factories = new ArrayList<>();
        for (int i = 1; i <= options.amount(); i++) {
            Pair<String, String> userPassword;

            if (accounts == null) {
                userPassword = new Pair<>(String.format(options.botNameFormat(), i), "");
            } else {
                if (accounts.size() <= i) {
                    logger.warn("Bot amount is set higher than accounts are available. Limiting bot amount to {}", accounts.size());
                    break;
                }

                String[] lines = accounts.get(i).split(":");

                if (lines.length == 1) {
                    userPassword = new Pair<>(lines[0], "");
                } else if (lines.length == 2) {
                    userPassword = new Pair<>(lines[0], lines[1]);
                } else {
                    userPassword = new Pair<>(String.format(options.botNameFormat(), i), "");
                }
            }

            Optional<MinecraftProtocol> optional = authenticate(options, userPassword.left(), userPassword.right(), Proxy.NO_PROXY);
            if (optional.isEmpty()) {
                logger.warn("The account " + userPassword.left() + " failed to authenticate! (skipping it) Check above logs for further information.");
                continue;
            }
            MinecraftProtocol protocol = optional.get();

            // Make sure this options is set to false, otherwise it will cause issues with ViaVersion
            protocol.setUseDefaultListeners(false);

            Logger logger = LoggerFactory.getLogger(protocol.getProfile().getName());
            ProxyBotData proxyBotData = null;
            if (!proxyCache.isEmpty()) {
                proxyIterator = fromStartIfNoNext(proxyIterator, proxyCache);
                BotProxy proxy = proxyIterator.next();

                if (options.accountsPerProxy() > 0) {
                    proxyUseMap.putIfAbsent(proxy, new AtomicInteger());
                    while (proxyUseMap.get(proxy).get() >= options.accountsPerProxy()) {
                        proxyIterator = fromStartIfNoNext(proxyIterator, proxyCache);
                        proxy = proxyIterator.next();
                        proxyUseMap.putIfAbsent(proxy, new AtomicInteger());

                        if (!proxyIterator.hasNext() && proxyUseMap.get(proxy).get() >= options.accountsPerProxy()) {
                            break;
                        }
                    }

                    proxyUseMap.get(proxy).incrementAndGet();

                    if (proxyUseMap.size() == proxyCache.size() && isFull(proxyUseMap, options.accountsPerProxy())) {
                        logger.warn("All proxies already in use! Limiting amount of bots to {}", i - 1);
                        break;
                    }
                }

                proxyBotData = ProxyBotData.of(proxy.username(), proxy.password(), proxy.address(), options.proxyType());
            }

            factories.add(new BotConnectionFactory(this, options, logger, protocol, options.authService(), proxyBotData));
        }

        if (proxyCache.isEmpty()) {
            logger.info("Starting attack at {} with {} bots", options.host(), botConnections.size());
        } else {
            logger.info("Starting attack at {} with {} bots and {} proxies", options.host(), botConnections.size(), proxyUseMap.size());
        }

        ServerWreckerAPI.postEvent(new AttackStartEvent());

        for (BotConnectionFactory botConnectionFactory : factories) {
            try {
                TimeUnit.MILLISECONDS.sleep(options.joinDelayMs());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            while (attackState.isPaused()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Stop the bot in case the user aborted the attack
            if (attackState.isStopped()) {
                break;
            }

            botConnectionFactory.logger().info("Connecting...");

            CompletableFuture<BotConnection> future = botConnectionFactory.connect();

            if (options.waitEstablished()) {
                this.botConnections.add(future.join());
            } else {
                future.thenAccept(this.botConnections::add);
            }
        }
    }

    public Optional<MinecraftProtocol> authenticate(SWOptions options, String username, String password, Proxy authProxy) {
        if (password.isEmpty()) {
            return Optional.of(new MinecraftProtocol(username));
        } else {
            try {
                AuthenticationService authService = switch (options.authService()) {
                    case OFFLINE -> new OfflineAuthenticationService();
                    case MICROSOFT -> new MsaAuthenticationService(serviceServerConfig.get("clientId"));
                };

                authService.setUsername(username);
                authService.setPassword(password);
                authService.setProxy(authProxy);

                authService.login();

                GameProfile profile = authService.getSelectedProfile();
                String accessToken = authService.getAccessToken();

                return Optional.of(new MinecraftProtocol(profile, accessToken));
            } catch (RequestException e) {
                logger.warn("Failed to authenticate {}! ({})", username, e.getMessage(), e);
                return Optional.empty();
            }
        }
    }

    public void stop() {
        this.attackState = AttackState.STOPPED;
        botConnections.forEach(BotConnection::disconnect);
        botConnections.clear();
        ServerWreckerAPI.postEvent(new AttackEndEvent());
    }

    private boolean isFull(Map<BotProxy, AtomicInteger> map, int limit) {
        for (Map.Entry<BotProxy, AtomicInteger> entry : map.entrySet()) {
            if (entry.getValue().get() <= limit) {
                return false;
            }
        }

        return true;
    }

    private Iterator<BotProxy> fromStartIfNoNext(Iterator<BotProxy> iterator, List<BotProxy> proxyList) {
        return iterator.hasNext() ? iterator : proxyList.listIterator();
    }

    public void setupLogging(Level level) {
        LogUtil.setLevel(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME, level);
        LogUtil.setLevel(logger, level);
        LogUtil.setLevel("io.netty", level);
        LogUtil.setLevel("org.pf4j", level);
    }
}
