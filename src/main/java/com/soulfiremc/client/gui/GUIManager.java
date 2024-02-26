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
package com.soulfiremc.client.gui;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.reflect.Modules;
import com.soulfiremc.client.ClientCommandManager;
import com.soulfiremc.client.SFTerminalConsole;
import com.soulfiremc.client.grpc.RPCClient;
import com.soulfiremc.client.settings.SettingsManager;
import com.soulfiremc.util.SFPathConstants;
import com.soulfiremc.util.ShutdownManager;

@Slf4j
@Getter
public class GUIManager {
  private final RPCClient rpcClient;
  private final ClientCommandManager clientCommandManager;
  private final Injector injector =
      new InjectorBuilder().addDefaultHandlers("com.soulfiremc").create();
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
      Files.createDirectories(SFPathConstants.PROFILES_FOLDER);
    } catch (IOException e) {
      log.error("Failed to create profiles folder!", e);
    }

    SFTerminalConsole.setupTerminalConsole(threadPool, shutdownManager, clientCommandManager);

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
    SwingUtilities.invokeLater(
        () -> {
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

      var classNameVariable =
          MethodHandles.privateLookupIn(xToolkit.getClass(), MethodHandles.lookup())
              .findStaticVarHandle(xToolkit.getClass(), "awtAppClassName", String.class);

      classNameVariable.set("SoulFire");
    } catch (Exception e) {
      log.error("Failed to set app title!", e);
    }
  }

  public void shutdown() {
    shutdownManager.shutdownSoftware(true);
  }

  public void browse(URI uri) {
    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
      log.error("Desktop not supported!");
      return;
    }

    threadPool.submit(
        () -> {
          try {
            Desktop.getDesktop().browse(uri);
          } catch (IOException e) {
            log.error("Failed to open browser!", e);
          }
        });
  }
}
