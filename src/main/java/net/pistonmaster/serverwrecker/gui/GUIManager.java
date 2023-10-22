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
import javafx.embed.swing.JFXPanel;
import lombok.Getter;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.grpc.RPCClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class GUIManager {
    private final RPCClient rpcClient;
    private final Injector injector;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Logger logger = LoggerFactory.getLogger(GUIManager.class);

    public GUIManager(ServerWrecker serverWrecker, RPCClient rpcClient) {
        this.rpcClient = rpcClient;
        this.injector = serverWrecker.getInjector(); // TODO: Separate injector for GUI
        injector.register(GUIManager.class, this);
    }

    public void initGUI() {
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

    public void setAppTitle() {
        try {
            var xToolkit = Toolkit.getDefaultToolkit();
            if (!xToolkit.getClass().getName().equals("sun.awt.X11.XToolkit")) {
                return;
            }

            var CLASS_NAME_VARIABLE = MethodHandles
                    .privateLookupIn(xToolkit.getClass(), MethodHandles.lookup())
                    .findStaticVarHandle(xToolkit.getClass(), "awtAppClassName", String.class);

            CLASS_NAME_VARIABLE.set("ServerWrecker");
        } catch (Exception e) {
            logger.error("Failed to set app title!", e);
        }
    }
}
