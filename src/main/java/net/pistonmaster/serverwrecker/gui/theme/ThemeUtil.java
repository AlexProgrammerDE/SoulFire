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
package net.pistonmaster.serverwrecker.gui.theme;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.util.SystemInfo;
import net.pistonmaster.serverwrecker.ServerWreckerBootstrap;
import net.pistonmaster.serverwrecker.settings.lib.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ThemeUtil {
    public static final Path THEME_PATH = ServerWreckerBootstrap.DATA_FOLDER.resolve("theme.json");
    public static final SettingsManager THEME_MANAGER = new SettingsManager(Map.of("theme", ThemeSettings.class));
    public static final ThemeProvider THEME_PROVIDER = new ThemeProvider(FlatDarculaLaf.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeUtil.class);

    static {
        THEME_MANAGER.registerDuplex(ThemeSettings.class, THEME_PROVIDER);

        if (Files.exists(THEME_PATH)) {
            try {
                THEME_MANAGER.loadProfile(THEME_PATH);
            } catch (IOException e) {
                LOGGER.error("Failed to load theme settings!", e);
            }
        }
    }

    private ThemeUtil() {
    }

    /**
     * Apply the current theme that is set in the settings.
     * This will also save the theme to the settings.
     * You need to invoke SwingUtilities.updateComponentTreeUI(frame); after this method.
     */
    public static void setLookAndFeel() {
        var themeSettings = THEME_MANAGER.collectSettings().get(ThemeSettings.class);
        if (themeSettings.themeClass().equals(UIManager.getLookAndFeel().getClass().getName())) {
            return;
        }

        try {
            var theme = Class.forName(themeSettings.themeClass())
                    .asSubclass(BasicLookAndFeel.class).getDeclaredConstructor().newInstance();

            FlatAnimatedLafChange.showSnapshot();

            UIManager.setLookAndFeel(theme);

            FlatLaf.updateUI();

            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        } catch (UnsupportedLookAndFeelException | ReflectiveOperationException e) {
            LOGGER.error("Failed to set theme!", e);
        }

        try {
            THEME_MANAGER.saveProfile(THEME_PATH);
        } catch (IOException e) {
            LOGGER.error("Failed to save theme settings!", e);
        }
    }

    public static void initFlatLaf() {
        if (SystemInfo.isMacOS) {
            // Use top screen menu bar on macOS
            System.setProperty("apple.laf.useScreenMenuBar", "true");

            // Set name in top menu bar
            System.setProperty("apple.awt.application.name", "ServerWrecker");

            // Color the frame
            System.setProperty("apple.awt.application.appearance", "system");
        } else if (SystemInfo.isLinux) {
            // Make window decorations like on windows
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        }
    }
}
