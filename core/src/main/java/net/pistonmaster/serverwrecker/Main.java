/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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

import net.pistonmaster.serverwrecker.api.SWPluginLoader;
import net.pistonmaster.serverwrecker.gui.MainFrame;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;
import picocli.CommandLine;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> ServerWrecker.getLogger().error(throwable.getMessage(), throwable));

        Path dataFolder = initConfigDir();

        if (GraphicsEnvironment.isHeadless() || args.length > 0) {
            runHeadless(args, dataFolder);
        } else {
            SWPluginLoader.initPlugins(dataFolder);
            new ServerWrecker().getInjector().getSingleton(MainFrame.class);
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
        int exitCode = new CommandLine(new CommandDefinition(dataFolder)).execute(args);
        System.exit(exitCode);
    }
}
