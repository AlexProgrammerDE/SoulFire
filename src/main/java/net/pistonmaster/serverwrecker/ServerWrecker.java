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
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
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
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.AttackEndEvent;
import net.pistonmaster.serverwrecker.api.event.AttackStartEvent;
import net.pistonmaster.serverwrecker.builddata.BuildData;
import net.pistonmaster.serverwrecker.common.*;
import net.pistonmaster.serverwrecker.gui.navigation.SettingsPanel;
import net.pistonmaster.serverwrecker.logging.LogUtil;
import net.pistonmaster.serverwrecker.mojangdata.AssetData;
import net.pistonmaster.serverwrecker.protocol.Bot;
import net.pistonmaster.serverwrecker.protocol.OfflineAuthenticationService;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.bot.block.GlobalBlockPalette;
import net.pistonmaster.serverwrecker.viaversion.SWViaLoader;
import net.pistonmaster.serverwrecker.viaversion.platform.*;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class ServerWrecker {
    public static final String PROJECT_NAME = "ServerWrecker";
    public static final String VERSION = "0.0.2";
    @Getter
    private static final Logger logger = LoggerFactory.getLogger(PROJECT_NAME);
    @Getter
    private final Injector injector = new InjectorBuilder()
            .addDefaultHandlers("net.pistonmaster.serverwrecker")
            .create();
    private final List<Bot> bots = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final List<BotProxy> passWordProxies = new ArrayList<>();
    private final Map<String, String> serviceServerConfig = new HashMap<>();
    private final Gson gson = new Gson();
    private final AssetData assetData;
    private final GlobalBlockPalette globalBlockPalette;
    private boolean running = false;
    @Setter
    private boolean paused = false;
    @Setter
    private List<String> accounts;
    @Setter
    private ServiceServer serviceServer = ServiceServer.MOJANG;

    public ServerWrecker(Path dataFolder) {
        // Register into injector
        injector.register(ServerWrecker.class, this);

        // Init API
        ServerWreckerAPI.setServerWrecker(this);

        setupLogging(Level.INFO);

        // Load assets
        JsonObject blocks;
        try (InputStream stream = ServerWrecker.class.getResourceAsStream("/minecraft/blocks.json")) {
            assert stream != null;
            blocks = gson.fromJson(new InputStreamReader(stream), JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JsonObject translations;
        try (InputStream stream = ServerWrecker.class.getResourceAsStream("/minecraft/en_us.json")) {
            assert stream != null;
            translations = gson.fromJson(new InputStreamReader(stream), JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assetData = new AssetData(blocks, translations);

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
        });

        ViaManagerImpl manager = (ViaManagerImpl) Via.getManager();
        manager.init();

        manager.getPlatform().getConf().setCheckForUpdates(false);

        manager.onServerLoaded();

        SettingsPanel settingsPanel = injector.getIfAvailable(SettingsPanel.class);
        if (settingsPanel != null) {
            settingsPanel.registerVersions();
        }

        initPlugins(dataFolder.resolve("plugins"));
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

    public void start(SWOptions options) {
        if (options.debug()) {
            Via.getManager().debugHandler().setEnabled(true);
            setupLogging(Level.DEBUG);
        } else {
            Via.getManager().debugHandler().setEnabled(false);
            setupLogging(Level.INFO);
        }

        this.running = true;

        List<BotProxy> proxyCache = passWordProxies.isEmpty() ? Collections.emptyList() : ImmutableList.copyOf(passWordProxies);
        Iterator<BotProxy> proxyIterator = proxyCache.listIterator();
        Map<BotProxy, AtomicInteger> proxyUseMap = new HashMap<>();

        for (int i = 1; i <= options.amount(); i++) {
            Pair<String, String> userPassword;

            if (accounts == null) {
                userPassword = new Pair<>(String.format(options.botNameFormat(), i), "");
            } else {
                if (accounts.size() <= i) {
                    logger.warn("Amount is higher than the name list size. Limiting amount size now...");
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

            Optional<MinecraftProtocol> optional = authenticate(userPassword.left(), userPassword.right(), Proxy.NO_PROXY);
            if (optional.isEmpty()) {
                logger.warn("The account " + userPassword.left() + " failed to authenticate! (skipping it) Check above logs for further information.");
                continue;
            }
            MinecraftProtocol protocol = optional.get();

            // Make sure this options is set to false, otherwise it will cause issues with ViaVersion
            protocol.setUseDefaultListeners(false);

            Logger logger = LoggerFactory.getLogger(protocol.getProfile().getName());
            Bot bot;
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
                        logger.warn("All proxies in use now! Limiting amount size now...");
                        break;
                    }
                }

                ProxyBotData proxyBotData = ProxyBotData.of(proxy.username(), proxy.password(), proxy.address(), options.proxyType());
                bot = new Bot(this, options, logger, protocol, serviceServer, proxyBotData);
            } else {
                bot = new Bot(this, options, logger, protocol, serviceServer, null);
            }

            this.bots.add(bot);
        }

        if (proxyCache.isEmpty()) {
            logger.info("Starting attack at {} with {} bots", options.hostname(), bots.size());
        } else {
            logger.info("Starting attack at {} with {} bots and {} proxies", options.hostname(), bots.size(), proxyUseMap.size());
        }

        ServerWreckerAPI.postEvent(new AttackStartEvent());

        for (Bot bot : bots) {
            try {
                TimeUnit.MILLISECONDS.sleep(options.joinDelayMs());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            while (paused) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Stop the bot in case the user aborted the attack
            if (!running) {
                break;
            }

            bot.getLogger().info("Connecting...");

            bot.connect(options.hostname(), options.port(), new SessionDataManager(options, bot.getLogger(), bot, this));
        }
    }

    public Optional<MinecraftProtocol> authenticate(String username, String password, Proxy proxy) {
        if (password.isEmpty()) {
            return Optional.of(new MinecraftProtocol(username));
        } else {
            try {
                AuthenticationService authService = switch (serviceServer) {
                    case OFFLINE -> new OfflineAuthenticationService();
                    case MOJANG -> new MojangAuthenticationService();
                    case MICROSOFT -> new MsaAuthenticationService(serviceServerConfig.get("clientId"));
                };

                authService.setUsername(username);
                authService.setPassword(password);
                authService.setProxy(proxy);

                authService.login();

                GameProfile profile = authService.getSelectedProfile();
                String accessToken = authService.getAccessToken();

                return Optional.of(new MinecraftProtocol(profile, accessToken));
            } catch (RequestException e) {
                logger.warn("Failed to authenticate " + username + "! (" + e.getMessage() + ")", e);
                return Optional.empty();
            }
        }
    }

    public void stop() {
        this.running = false;
        bots.forEach(Bot::disconnect);
        bots.clear();
        ServerWreckerAPI.postEvent(new AttackEndEvent());
    }

    private boolean isFull(Map<BotProxy, AtomicInteger> map, int limit) {
        for (Map.Entry<BotProxy, AtomicInteger> entry : map.entrySet()) {
            if (entry.getValue().get() < limit) {
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
