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
package net.pistonmaster.soulfire.client.gui;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.reflect.Modules;
import net.pistonmaster.soulfire.client.ClientCommandManager;
import net.pistonmaster.soulfire.client.SWTerminalConsole;
import net.pistonmaster.soulfire.client.grpc.RPCClient;
import net.pistonmaster.soulfire.client.settings.SettingsManager;
import net.pistonmaster.soulfire.util.SWPathConstants;
import net.pistonmaster.soulfire.util.ShutdownManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Getter
public class GUIManager {
    public static final Queue<Runnable> MAIN_THREAD_QUEUE = new ConcurrentLinkedQueue<>();
    private final RPCClient rpcClient;
    private final ClientCommandManager clientCommandManager;
    private final Injector injector = new InjectorBuilder()
            .addDefaultHandlers("net.pistonmaster.soulfire")
            .create();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final ShutdownManager shutdownManager = new ShutdownManager(this::shutdownHook);
    private final SettingsManager settingsManager = new SettingsManager();

    public GUIManager(RPCClient rpcClient) {
        this.rpcClient = rpcClient;
        injector.register(GUIManager.class, this);
        injector.register(RPCClient.class, rpcClient);
        injector.register(ShutdownManager.class, shutdownManager);
        injector.register(SettingsManager.class, settingsManager);

        this.clientCommandManager = injector.getSingleton(ClientCommandManager.class);
    }

    public static void injectTheme() {
        ThemeUtil.initFlatLaf();
        ThemeUtil.setLookAndFeel();
    }

    public static void loadGUIProperties() {
        GUIClientProps.loadSettings();
    }

    public void initGUI() {
        try {
            Files.createDirectories(SWPathConstants.PROFILES_FOLDER);
        } catch (IOException e) {
            log.error("Failed to create profiles folder!", e);
        }

        SWTerminalConsole.setupTerminalConsole(threadPool, shutdownManager, clientCommandManager);

        // Override the title in AWT (GNOME displays the class name otherwise)
        setAppTitle();

        // Inject and open the GUI
        var guiFrame = new GUIFrame();

        injector.register(GUIFrame.class, guiFrame);

        guiFrame.initComponents(injector);

        log.info("Opening GUI!");

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

            CLASS_NAME_VARIABLE.set("SoulFire");
        } catch (Exception e) {
            log.error("Failed to set app title!", e);
        }
    }

    public void shutdown() {
        shutdownManager.shutdownSoftware(true);
    }
}
