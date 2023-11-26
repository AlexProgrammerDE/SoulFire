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

import io.netty.util.ResourceLeakDetector;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import net.lenni0451.reflect.Agents;
import net.pistonmaster.serverwrecker.api.MixinExtension;
import net.pistonmaster.serverwrecker.builddata.BuildData;
import net.pistonmaster.serverwrecker.settings.DevSettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsHolder;
import net.pistonmaster.serverwrecker.util.CustomClassProvider;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.fusesource.jansi.AnsiConsole;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This class prepares the earliest work possible, such as loading mixins and
 * setting up logging.
 */
public class ServerWreckerBootstrap {
    public static final Path DATA_FOLDER = Path.of(System.getProperty("user.home"), ".serverwrecker");
    public static final Path PLUGINS_FOLDER = DATA_FOLDER.resolve("plugins");
    public static final PluginManager PLUGIN_MANAGER = new JarPluginManager(PLUGINS_FOLDER);
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerWreckerBootstrap.class);

    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        // If Velocity's natives are being extracted to a different temporary directory, make sure the
        // Netty natives are extracted there as well
        if (System.getProperty("velocity.natives-tmpdir") != null) {
            System.setProperty("io.netty.native.workdir", System.getProperty("velocity.natives-tmpdir"));
        }

        // Disable the resource leak detector by default as it reduces performance. Allow the user to
        // override this if desired.
        if (System.getProperty("io.netty.leakDetection.level") == null) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        }
    }

    private ServerWreckerBootstrap() {
    }

    @SuppressWarnings("unused")
    public static void bootstrap(String[] args, List<ClassLoader> classLoaders) {
        injectAnsi();
        setupLogging(SettingsHolder.EMPTY);

        injectExceptionHandler();

        initPlugins(classLoaders);

        // We may split client and server mixins in the future
        var runServer = GraphicsEnvironment.isHeadless() || args.length > 0;

        injectMixinsAndRun(runServer, args);
    }

    private static void injectMixinsAndRun(boolean runServer, String[] args) {
        var mixinPaths = new HashSet<String>();
        PLUGIN_MANAGER.getExtensions(MixinExtension.class).forEach(mixinExtension -> {
            for (var mixinPath : mixinExtension.getMixinPaths()) {
                if (mixinPaths.add(mixinPath)) {
                    LOGGER.info("Added mixin \"{}\"", mixinPath);
                } else {
                    LOGGER.warn("Mixin path \"{}\" is already added!", mixinPath);
                }
            }
        });

        var classLoaders = new ArrayList<ClassLoader>();
        classLoaders.add(ServerWreckerBootstrap.class.getClassLoader());
        PLUGIN_MANAGER.getPlugins().forEach(pluginWrapper ->
                classLoaders.add(pluginWrapper.getPluginClassLoader()));

        var classProvider = new CustomClassProvider(classLoaders);
        var transformerManager = new TransformerManager(classProvider);
        transformerManager.addTransformerPreprocessor(new MixinsTranslator());
        mixinPaths.forEach(transformerManager::addTransformer);

        try {
            transformerManager.hookInstrumentation(Agents.getInstrumentation());
            LOGGER.info("Used Runtime Agent to inject mixins");

            postMixinMain(runServer, args);
        } catch (ReflectiveOperationException | IOException t) {
            LOGGER.error("Failed to inject mixins", t);
            System.exit(1);
        }
    }

    private static void postMixinMain(boolean runServer, String[] args) {
        var port = getAvailablePort();
        if (runServer) {
            LOGGER.info("Starting server on port {}", port);
            ServerWreckerLoader.runHeadless(port, args);
        } else {
            LOGGER.info("Starting GUI and server on port {}", port);
            ServerWreckerLoader.injectTheme();
            ServerWreckerLoader.loadGUIProperties();

            ServerWreckerLoader.runGUI(port);
        }
    }

    public static void injectExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.error("Exception in thread {}", thread.getName());
            //noinspection CallToPrintStackTrace
            throwable.printStackTrace();
        });
    }

    private static void initPlugins(List<ClassLoader> classLoaders) {
        try {
            Files.createDirectories(PLUGINS_FOLDER);
        } catch (IOException e) {
            LOGGER.error("Failed to create plugin directory", e);
        }

        // Prepare the plugin manager
        PLUGIN_MANAGER.setSystemVersion(BuildData.VERSION);

        // Load all plugins available
        PLUGIN_MANAGER.loadPlugins();
        PLUGIN_MANAGER.startPlugins();

        for (var plugin : PLUGIN_MANAGER.getPlugins()) {
            classLoaders.add(plugin.getPluginClassLoader());
        }
    }

    /**
     * RGB support for terminals.
     */
    private static void injectAnsi() {
        if (System.console() == null) {
            return;
        }

        AnsiConsole.systemInstall();
    }

    public static void setupLogging(SettingsHolder settingsHolder) {
        var level = settingsHolder.get(DevSettings.CORE_DEBUG) ? Level.DEBUG : Level.INFO;
        var nettyLevel = settingsHolder.get(DevSettings.NETTY_DEBUG) ? Level.DEBUG : Level.INFO;
        var grpcLevel = settingsHolder.get(DevSettings.GRPC_DEBUG) ? Level.DEBUG : Level.INFO;
        Configurator.setRootLevel(level);
        Configurator.setLevel("io.netty", nettyLevel);
        Configurator.setLevel("io.grpc", grpcLevel);
    }

    private static int getAvailablePort() {
        var initialPort = 38765;

        while (true) {
            try {
                var serverSocket = new ServerSocket(initialPort);
                serverSocket.close();
                break; // Port is available, exit the loop
            } catch (IOException e) {
                LOGGER.info("Port {} is already in use, trying next port...", initialPort);
                initialPort++; // Increment the port number and try again
            }
        }

        return initialPort;
    }
}
