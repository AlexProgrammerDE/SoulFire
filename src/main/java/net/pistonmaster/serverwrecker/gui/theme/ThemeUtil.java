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
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.settings.lib.SettingsManager;

import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ThemeUtil {
    public static final Path THEME_PATH = ServerWrecker.DATA_FOLDER.resolve("theme.json");
    public static final SettingsManager THEME_MANAGER = new SettingsManager(ThemeSettings.class);
    public static final ThemeProvider THEME_PROVIDER = new ThemeProvider(FlatDarculaLaf.class);

    static {
        THEME_MANAGER.registerDuplex(ThemeSettings.class, THEME_PROVIDER);

        if (Files.exists(THEME_PATH)) {
            try {
                THEME_MANAGER.loadProfile(THEME_PATH);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Apply the current theme that is set in the settings.
     * This will also save the theme to the settings.
     * You need to invoke SwingUtilities.updateComponentTreeUI(frame); after this method.
     */
    public static void setLookAndFeel() {
        ThemeSettings themeSettings = THEME_MANAGER.collectSettings().get(ThemeSettings.class);
        if (themeSettings.themeClass().equals(UIManager.getLookAndFeel().getClass().getName())) {
            return;
        }

        try {
            BasicLookAndFeel theme = Class.forName(themeSettings.themeClass())
                    .asSubclass(BasicLookAndFeel.class).getDeclaredConstructor().newInstance();

            UIManager.setLookAndFeel(theme);
        } catch (UnsupportedLookAndFeelException | ReflectiveOperationException e) {
            e.printStackTrace();
        }

        try {
            Files.createDirectories(ServerWrecker.DATA_FOLDER);

            THEME_MANAGER.saveProfile(THEME_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
