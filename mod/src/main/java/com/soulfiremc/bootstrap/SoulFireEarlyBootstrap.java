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

import com.soulfiremc.shared.SFLogAppender;
import io.netty.util.ResourceLeakDetector;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.fusesource.jansi.AnsiConsole;

import java.security.Security;

@Slf4j
public class SoulFireEarlyBootstrap {
  public static void preMainBootstrap() {
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

    SFLogAppender.INSTANCE.start();

    AnsiConsole.systemInstall();

    sendFlagsInfo();

    injectExceptionHandler();
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

  private static void injectExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(
      (thread, throwable) -> log.atError().setCause(throwable).log("Exception in thread {}", thread.getName()));
  }
}
