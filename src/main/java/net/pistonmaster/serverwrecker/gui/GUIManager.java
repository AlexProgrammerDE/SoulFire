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
package net.pistonmaster.serverwrecker.gui;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import javafx.embed.swing.JFXPanel;
import lombok.Getter;
import net.lenni0451.reflect.Modules;
import net.pistonmaster.serverwrecker.command.SWTerminalConsole;
import net.pistonmaster.serverwrecker.command.ShutdownManager;
import net.pistonmaster.serverwrecker.grpc.RPCClient;
import net.pistonmaster.serverwrecker.settings.lib.SettingsManager;
import net.pistonmaster.serverwrecker.util.SWPathConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class GUIManager {
    private final RPCClient rpcClient;
    private final Injector injector = new InjectorBuilder()
            .addDefaultHandlers("net.pistonmaster.serverwrecker")
            .create();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Logger logger = LoggerFactory.getLogger(GUIManager.class);
    private final ShutdownManager shutdownManager = new ShutdownManager(this::shutdownHook);
    private final SettingsManager settingsManager = new SettingsManager();

    public GUIManager(RPCClient rpcClient) {
        this.rpcClient = rpcClient;
        injector.register(GUIManager.class, this);
    }

    public void initGUI() {
        try {
            Files.createDirectories(SWPathConstants.PROFILES_FOLDER);
        } catch (IOException e) {
            logger.error("Failed to create profiles folder!", e);
        }

        SWTerminalConsole.setupTerminalConsole(threadPool, shutdownManager, rpcClient);

        // Override the title in AWT (GNOME displays the class name otherwise)
        setAppTitle();

        // Initialize the JavaFX Platform
        new JFXPanel();

        // Inject and open the GUI
        var guiFrame = new GUIFrame();

        guiFrame.initComponents(injector);

        logger.info("Opening GUI!");

        SwingUtilities.invokeLater(() -> guiFrame.open(injector));
    }

    private void shutdownHook() {
        threadPool.shutdown();
        SwingUtilities.invokeLater(() -> {
            var frame = (GUIFrame) injector.getSingleton(GUIFrame.class);
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        });
    }

    public void setAppTitle() {
        try {
            var xToolkit = Toolkit.getDefaultToolkit();
            if (!xToolkit.getClass().getName().equals("sun.awt.X11.XToolkit")) {
                return;
            }

            // Force open this module
            Modules.openModule(xToolkit.getClass());

            var CLASS_NAME_VARIABLE = MethodHandles
                    .privateLookupIn(xToolkit.getClass(), MethodHandles.lookup())
                    .findStaticVarHandle(xToolkit.getClass(), "awtAppClassName", String.class);

            CLASS_NAME_VARIABLE.set("ServerWrecker");
        } catch (Exception e) {
            logger.error("Failed to set app title!", e);
        }
    }

    public void shutdown() {
        shutdownManager.shutdown(true);
    }
}
