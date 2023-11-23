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
import com.formdev.flatlaf.util.SystemInfo;
import net.pistonmaster.serverwrecker.gui.libs.HintManager;
import net.pistonmaster.serverwrecker.gui.navigation.CardsContainer;
import net.pistonmaster.serverwrecker.gui.navigation.ControlPanel;

import javax.swing.*;
import java.awt.*;

public class GUIFrame extends JFrame {
    public static final String MAIN_MENU = "MainMenu";

    public GUIFrame() {
        super("ServerWrecker");
    }

    public void initComponents(Injector injector) {
        if (SystemInfo.isMacOS) {
            // Hide window title because we want to avoid dark-mode name issues
            getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }

        setResizable(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setJMenuBar(injector.getSingleton(SWMenuBar.class));

        setLayout(new CardLayout());
        add(injector.newInstance(MainPanel.class), MAIN_MENU);

        pack();

        // Calculate 16:9 width from height
        var height = getHeight();
        var aspectRatio = 16.0 / 9.0;
        var width = (int) (height * aspectRatio);

        setSize(width, height);
        setMinimumSize(new Dimension(width, height));
    }

    public void open(Injector injector) {
        setVisible(true);

        // User says they are a first time user that wants hints
        if (GUIClientProps.getBoolean("firstTimeUser", false)) {
            SwingUtilities.invokeLater(() -> showHints(injector));
            return;
        }

        // Ask whether the user wants hints
        if (GUIClientProps.getBoolean("firstRun", true)) {
            var result = JOptionPane.showConfirmDialog(
                    this,
                    "Is this your first time using ServerWrecker? If yes, we can help you to get started with a few hints. :D",
                    "ServerWrecker First Run",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                GUIClientProps.setBoolean("firstTimeUser", true);
                GUIClientProps.setBoolean("firstRun", false);

                requestFocusInWindow();
                SwingUtilities.invokeLater(() -> showHints(injector));
            } else if (result == JOptionPane.NO_OPTION || result == JOptionPane.CLOSED_OPTION) {
                GUIClientProps.setBoolean("firstTimeUser", false);
                GUIClientProps.setBoolean("firstRun", false);
            }
        }
    }

    public static void showHints(Injector injector) {
        var logPanel = injector.getSingleton(LogPanel.class);
        var cardContainer = injector.getSingleton(CardsContainer.class);

        var commandsHint = new HintManager.Hint(
                "Use \"help\" to get a list of all commands.",
                (Component) logPanel.getClientProperty("log-panel-command-input"),
                SwingConstants.TOP, "hint.commandInput", null);

        var controlsHint = new HintManager.Hint(
                "Here you can start, pause and stop a attack.",
                injector.getSingleton(ControlPanel.class),
                SwingConstants.TOP, "hint.controls", commandsHint);

        var addonsHint = new HintManager.Hint(
                "Click to configure addons to make the attack more effective.",
                (Component) cardContainer.getClientProperty("addon-menu-button"),
                SwingConstants.BOTTOM, "hint.addonsButton", controlsHint);

        var proxyHint = new HintManager.Hint(
                "Click to import HTTP, SOCKS4 and SOCKS5 proxies",
                (Component) cardContainer.getClientProperty("proxy-menu-button"),
                SwingConstants.LEFT, "hint.proxyButton", addonsHint);

        var accountsHint = new HintManager.Hint(
                "Click to configure the bot offline-mode name format or bring your own accounts.",
                (Component) cardContainer.getClientProperty("account-menu-button"),
                SwingConstants.RIGHT, "hint.accountsButton", proxyHint);

        var settingsHint = new HintManager.Hint(
                "Click to configure host, port, version and more.",
                (Component) cardContainer.getClientProperty("settings-menu-button"),
                SwingConstants.BOTTOM, "hint.settingsButton", accountsHint);

        var logsHint = new HintManager.Hint(
                "Here are all logs of the software displayed. You can select text and right click it to upload it to pastes.dev",
                logPanel,
                SwingConstants.LEFT, "hint.logPanel", settingsHint);

        HintManager.showHint(logsHint);
    }
}
