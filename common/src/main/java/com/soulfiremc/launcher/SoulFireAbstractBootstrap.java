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
package com.soulfiremc.launcher;

import com.soulfiremc.api.MixinExtension;
import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.util.CustomClassProvider;
import com.soulfiremc.util.PortHelper;
import com.soulfiremc.util.SFPathConstants;
import io.netty.util.ResourceLeakDetector;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import net.lenni0451.reflect.Agents;
import org.fusesource.jansi.AnsiConsole;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;

/**
 * This class prepares the earliest work possible, such as loading mixins and setting up logging.
 */
@Slf4j
public abstract class SoulFireAbstractBootstrap {
  public static final Instant START_TIME = Instant.now();
  public static final PluginManager PLUGIN_MANAGER =
    new JarPluginManager(SFPathConstants.PLUGINS_FOLDER);

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

  protected SoulFireAbstractBootstrap() {}

  public static int getRPCPort(int defaultPort) {
    return Integer.getInteger("sf.grpc.port", defaultPort);
  }

  public static int getRandomRPCPort() {
    var port = getRPCPort(0);

    return port == 0 ? PortHelper.getRandomAvailablePort() : port;
  }

  public static String getRPCHost(String defaultHost) {
    return System.getProperty("sf.grpc.host", defaultHost);
  }

  public static void injectExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(
      (thread, throwable) -> {
        log.error("Exception in thread {}", thread.getName());
        //noinspection CallToPrintStackTrace
        throwable.printStackTrace();
      });
  }

  private static void initPlugins(List<ClassLoader> classLoaders) {
    try {
      Files.createDirectories(SFPathConstants.PLUGINS_FOLDER);
    } catch (IOException e) {
      log.error("Failed to create plugin directory", e);
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

  protected void internalBootstrap(String[] args, List<ClassLoader> classLoaders) {
    AnsiConsole.systemInstall();

    injectExceptionHandler();

    initPlugins(classLoaders);

    injectMixinsAndRun(args);
  }

  private void injectMixinsAndRun(String[] args) {
    var mixinPaths = new HashSet<String>();
    PLUGIN_MANAGER
      .getExtensions(MixinExtension.class)
      .forEach(
        mixinExtension -> {
          for (var mixinPath : mixinExtension.getMixinPaths()) {
            if (mixinPaths.add(mixinPath)) {
              log.info("Added mixin \"{}\"", mixinPath);
            } else {
              log.warn("Mixin path \"{}\" is already added!", mixinPath);
            }
          }
        });

    var classLoaders = new ArrayList<ClassLoader>();
    classLoaders.add(SoulFireAbstractBootstrap.class.getClassLoader());
    PLUGIN_MANAGER
      .getPlugins()
      .forEach(pluginWrapper -> classLoaders.add(pluginWrapper.getPluginClassLoader()));

    var classProvider = new CustomClassProvider(classLoaders);
    var transformerManager = new TransformerManager(classProvider);
    transformerManager.addTransformerPreprocessor(new MixinsTranslator());
    mixinPaths.forEach(transformerManager::addTransformer);

    try {
      transformerManager.hookInstrumentation(Agents.getInstrumentation());
      log.info("Used Runtime Agent to inject mixins");

      this.postMixinMain(args);
    } catch (ReflectiveOperationException | IOException t) {
      log.error("Failed to inject mixins", t);
      System.exit(1);
    }
  }

  protected abstract void postMixinMain(String[] args);
}
