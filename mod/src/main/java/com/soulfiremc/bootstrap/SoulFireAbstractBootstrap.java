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
package com.soulfiremc.bootstrap;

import com.soulfiremc.server.api.InternalPluginClass;
import com.soulfiremc.server.util.PortHelper;
import com.soulfiremc.server.util.log4j.SFLogAppender;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.netty.util.ResourceLeakDetector;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.reflect.Fields;
import org.apache.logging.log4j.LogManager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * This class prepares the earliest work possible, such as loading mixins and setting up logging.
 */
@Slf4j
public abstract class SoulFireAbstractBootstrap {
  public static final Instant START_TIME = Instant.now();

  static {
    // Install the Log4J JUL bridge
    org.apache.logging.log4j.jul.LogManager.getLogManager().reset();

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

    Security.addProvider(new BouncyCastleProvider());
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

  private static void sendFlagsInfo() {
    if (Boolean.getBoolean("sf.flags.v1")) {
      return;
    }

    log.warn("We detected you are not using the recommended flags for SoulFire!");
    log.warn("Please add the following flags to your JVM arguments:");
    log.warn("-XX:+EnableDynamicAgentLoading -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:+ZGenerational -XX:+AlwaysActAsServerClassMachine -XX:+UseNUMA -XX:+UseFastUnorderedTimeStamps -XX:+UseVectorCmov -XX:+UseCriticalJavaThreadPriority -Dsf.flags.v1=true");
    log.warn("The startup command should look like: 'java -Xmx<ram> <flags> -jar <jarfile>'");
    log.warn("If you already have those flags or want to disable this warning, only add the '-Dsf.flags.v1=true' to your JVM arguments");
  }

  public static void injectExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(
      (thread, throwable) -> log.atError().setCause(throwable).log("Exception in thread {}", thread.getName()));
  }

  private void initPlugins() {
    try (var scanResult =
           new ClassGraph()
             .verbose()
             .enableAllInfo()
             .acceptPackages("com.soulfiremc.server.plugins")
             .scan()) {
      scanResult.getClassesWithAnnotation(InternalPluginClass.class)
        .stream()
        .sorted((a, b) -> {
          var aOrder = (int) a.getAnnotationInfo(InternalPluginClass.class).getParameterValues().getValue("order");
          var bOrder = (int) b.getAnnotationInfo(InternalPluginClass.class).getParameterValues().getValue("order");
          return Integer.compare(aOrder, bOrder);
        })
        .forEach(this::processRouteClass);
    } catch (Throwable t) {
      log.error("Failed to initialize plugins", t);
      throw new RuntimeException("Failed to initialize plugins", t);
    }
  }

  private void processRouteClass(ClassInfo classInfo) {
    try {
      var clazz = classInfo.loadClass();
      clazz.getConstructor().newInstance();
    } catch (Throwable t) {
      log.error("Failed to load plugin class {}", classInfo.getName(), t);
    }
  }

  @SneakyThrows
  protected void internalBootstrap(String[] args) {
    try {
      var forkJoinPoolFactory = new CustomThreadFactory();
      // Ensure the ForkJoinPool uses our custom thread factory
      Fields.set(ForkJoinPool.commonPool(), ForkJoinPool.class.getDeclaredField("factory"), forkJoinPoolFactory);
      Fields.set(null, ForkJoinPool.class.getDeclaredField("defaultForkJoinWorkerThreadFactory"), forkJoinPoolFactory);

      SFLogAppender.INSTANCE.start();

      AnsiConsole.systemInstall();

      sendFlagsInfo();

      injectFileProperties();

      injectExceptionHandler();

      initPlugins();

      injectMixinsAndRun(args);
    } catch (Throwable t) {
      // Ensure logs are written out on fatal errors
      log.error("Failed to start server", t);
      LogManager.shutdown();
      System.exit(1);
    }
  }

  private void injectFileProperties() {
    var optionsFile = getBaseDirectory().resolve("soulfire.properties");
    if (!Files.exists(optionsFile)) {
      return;
    }

    log.info("Loading options from {}", optionsFile);
    try (var scanner = new Scanner(optionsFile)) {
      var lineNumber = 0;
      while (scanner.hasNextLine()) {
        lineNumber++;
        var line = scanner.nextLine();
        if (line.startsWith("#") || line.isBlank()) {
          continue;
        }

        var parts = line.split("=", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
          log.warn("Invalid line in options file at line {}: {}", lineNumber, line);
          continue;
        }

        var parsedKey = parts[0].strip();
        var parsedValue = parts[1].strip();
        if (!parsedKey.startsWith("sf.")) {
          log.warn("Invalid key in options file at line {}: {}", lineNumber, parsedKey);
          continue;
        }

        System.setProperty(parsedKey, parsedValue);
      }
    } catch (IOException e) {
      log.error("Failed to read options file", e);
    }
  }

  public void injectMixinsAndRun(String[] args) {
    this.postMixinMain(args);
  }

  protected abstract void postMixinMain(String[] args);

  protected abstract Path getBaseDirectory();

  private static class CustomThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
      var thread = new CustomForkJoinWorkerThread(pool);
      thread.setContextClassLoader(Thread.currentThread().getContextClassLoader());
      return thread;
    }
  }

  private static class CustomForkJoinWorkerThread extends ForkJoinWorkerThread {
    protected CustomForkJoinWorkerThread(ForkJoinPool pool) {
      super(pool);
    }
  }
}
