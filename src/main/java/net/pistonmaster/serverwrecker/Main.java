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
import net.pistonmaster.serverwrecker.gui.MainFrame;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
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

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            System.exit(1);
        });

        Path dataFolder = initConfigDir();

        if (GraphicsEnvironment.isHeadless() || args.length > 0) {
            runHeadless(args, dataFolder);
        } else {
            MainFrame.setLookAndFeel();
            ServerWrecker serverWrecker = new ServerWrecker(dataFolder);

            serverWrecker.getInjector().getSingleton(MainFrame.class);
        }
    }

    private static Path initConfigDir() {
        Path dataDirectory = Path.of(System.getProperty("user.home"), ".serverwrecker");

        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataDirectory;
    }

    private static void runHeadless(String[] args, Path dataFolder) {
        CommandLine commandLine = new CommandLine(new CommandDefinition(dataFolder));
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
