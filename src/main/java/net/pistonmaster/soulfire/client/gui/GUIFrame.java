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
import com.formdev.flatlaf.util.SystemInfo;
import net.pistonmaster.soulfire.builddata.BuildData;
import net.pistonmaster.soulfire.client.gui.libs.HintManager;
import net.pistonmaster.soulfire.client.gui.navigation.CardsContainer;
import net.pistonmaster.soulfire.client.gui.navigation.ControlPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Objects;

public class GUIFrame extends JFrame {
    public static final String MAIN_MENU = "MainMenu";
    private Runnable hintFocusListener;

    public GUIFrame() {
        super("SoulFire " + BuildData.VERSION);
    }

    public static void showHints(Injector injector) {
        var logPanel = injector.getSingleton(LogPanel.class);
        var cardContainer = injector.getSingleton(CardsContainer.class);

        var commandsHint = new HintManager.Hint(
                "Use \"help\" to get a list of all commands.",
                (Component) Objects.requireNonNull(logPanel.getClientProperty("log-panel-command-input")),
                SwingConstants.TOP, "hint.commandInput", null);

        var controlsHint = new HintManager.Hint(
                "Here you can start, pause and stop a attack.",
                injector.getSingleton(ControlPanel.class),
                SwingConstants.TOP, "hint.controls", commandsHint);

        var pluginsHint = new HintManager.Hint(
                "Click to configure plugins to make the attack more effective.",
                (Component) Objects.requireNonNull(cardContainer.getClientProperty("plugin-menu-button")),
                SwingConstants.BOTTOM, "hint.pluginsButton", controlsHint);

        var proxyHint = new HintManager.Hint(
                "Click to import HTTP, SOCKS4 and SOCKS5 proxies",
                (Component) Objects.requireNonNull(cardContainer.getClientProperty("proxy-menu-button")),
                SwingConstants.RIGHT, "hint.proxyButton", pluginsHint);

        var accountsHint = new HintManager.Hint(
                "Click to configure the bot offline-mode name format or bring your own accounts.",
                (Component) Objects.requireNonNull(cardContainer.getClientProperty("account-menu-button")),
                SwingConstants.RIGHT, "hint.accountsButton", proxyHint);

        var settingsHint = new HintManager.Hint(
                "Click to configure host, port, version and more.",
                (Component) Objects.requireNonNull(cardContainer.getClientProperty("bot-button")),
                SwingConstants.BOTTOM, "hint.settingsButton", accountsHint);

        var logsHint = new HintManager.Hint(
                "Here are all logs of the software displayed. You can select text and right click it to upload it to pastes.dev",
                logPanel,
                SwingConstants.LEFT, "hint.logPanel", settingsHint);

        HintManager.showHint(logsHint);
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

        // Get screen height
        var gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        var displayHeight = gd.getDisplayMode().getHeight();

        // Calculate 16:9 width from height
        var minFrameHeight = displayHeight / 2;
        var aspectRatio = 16.0 / 9.0;
        var minFrameWidth = (int) (minFrameHeight * aspectRatio);

        setSize(minFrameWidth, minFrameHeight);
        setMinimumSize(new Dimension(minFrameWidth, minFrameHeight));

        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (hintFocusListener != null) {
                    hintFocusListener.run();
                    hintFocusListener = null;
                }
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
            }
        });

        setLocationRelativeTo(null);
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
                    "Is this your first time using SoulFire? If yes, we can help you to get started with a few hints. :D",
                    "SoulFire First Run",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                GUIClientProps.setBoolean("firstTimeUser", true);
                GUIClientProps.setBoolean("firstRun", false);

                // Wait for focus, then show hints or else the hints sometime glitch
                hintFocusListener = () -> showHints(injector);
                requestFocusInWindow();
            } else if (result == JOptionPane.NO_OPTION || result == JOptionPane.CLOSED_OPTION) {
                GUIClientProps.setBoolean("firstTimeUser", false);
                GUIClientProps.setBoolean("firstRun", false);
            }
        }
    }
}
