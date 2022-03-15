/*
 * ServerWrecker
 *
 * Copyright (C) 2021 ServerWrecker
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

import net.pistonmaster.serverwrecker.gui.MainFrame;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;
import picocli.CommandLine;

import java.awt.*;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> ServerWrecker.getLogger().error(throwable.getMessage(), throwable));

        File dataFolder = initConfigDir();

        if (GraphicsEnvironment.isHeadless() || args.length > 0) {
            runHeadless(args, dataFolder);
        } else {
            initPlugins(dataFolder);
            new MainFrame(ServerWrecker.getInstance());
        }
    }

    private static File initConfigDir() {
        File dataDirectory = new File(System.getProperty("user.home"), ".serverwrecker");

        //noinspection ResultOfMethodCallIgnored
        dataDirectory.mkdirs();

        return dataDirectory;
    }

    protected static void initPlugins(File dataFolder) {
        File pluginDir = new File(dataFolder, "plugins");

        //noinspection ResultOfMethodCallIgnored
        pluginDir.mkdirs();

        // create the plugin manager
        PluginManager pluginManager = new JarPluginManager(pluginDir.toPath());

        // start and load all plugins of application
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
    }

    private static void runHeadless(String[] args, File dataFolder) {
        int exitCode = new CommandLine(new CommandDefinition(dataFolder)).execute(args);
        System.exit(exitCode);
    }
}
