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

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import javafx.stage.FileChooser;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.gui.libs.JFXFileHelper;
import net.pistonmaster.serverwrecker.gui.popups.AboutPopup;
import net.pistonmaster.serverwrecker.gui.theme.ThemeUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SWMenuBar extends JMenuBar {
    private static final List<Class<? extends BasicLookAndFeel>> THEMES;

    static {
        List<Class<? extends BasicLookAndFeel>> tempThemes = new ArrayList<>(List.of(
                FlatDarculaLaf.class,
                FlatIntelliJLaf.class,
                FlatMacDarkLaf.class,
                FlatMacLightLaf.class,
                FlatOneDarkIJTheme.class
        ));
        THEMES = List.copyOf(tempThemes);
    }

    @Inject
    public SWMenuBar(ServerWrecker serverWrecker, JFrame frame) {
        JMenu fileMenu = new JMenu("File");
        JMenuItem loadProfile = new JMenuItem("Load Profile");
        loadProfile.addActionListener(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialDirectory(serverWrecker.getProfilesFolder().toFile());
            chooser.setTitle("Load Profile");
            chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("ServerWrecker profile", "json"));
            Path selectedFile = JFXFileHelper.showOpenDialog(chooser);

            if (selectedFile != null) {
                try {
                    serverWrecker.getSettingsManager().loadProfile(selectedFile);
                    serverWrecker.getLogger().info("Loaded profile!");
                } catch (IOException ex) {
                    serverWrecker.getLogger().warn("Failed to load profile!", ex);
                }
            }
        });

        fileMenu.add(loadProfile);
        JMenuItem saveProfile = new JMenuItem("Save Profile");
        saveProfile.addActionListener(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialDirectory(serverWrecker.getProfilesFolder().toFile());
            chooser.setTitle("Save Profile");
            chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("ServerWrecker profile", "json"));
            Path selectedFile = JFXFileHelper.showSaveDialog(chooser);

            if (selectedFile != null) {
                // Add .json if not present
                String path = selectedFile.toString();
                if (!path.endsWith(".json")) {
                    path += ".json";
                }

                try {
                    serverWrecker.getSettingsManager().saveProfile(Path.of(path));
                    serverWrecker.getLogger().info("Saved profile!");
                } catch (IOException ex) {
                    serverWrecker.getLogger().warn("Failed to save profile!", ex);
                }
            }
        });

        fileMenu.add(saveProfile);

        fileMenu.addSeparator();

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> serverWrecker.shutdown(true));
        fileMenu.add(exit);
        add(fileMenu);

        JMenu window = new JMenu("Window");
        JMenu themeSelector = new JMenu("Theme");
        for (Class<? extends BasicLookAndFeel> theme : THEMES) {
            JMenuItem themeItem = new JMenuItem(theme.getSimpleName());
            themeItem.addActionListener(e -> {
                ThemeUtil.THEME_PROVIDER.setThemeClass(theme);
                SwingUtilities.invokeLater(() -> {
                    ThemeUtil.setLookAndFeel();
                    SwingUtilities.updateComponentTreeUI(frame);
                });
            });
            themeSelector.add(themeItem);
        }
        window.add(themeSelector);
        add(window);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> {
            JPopupMenu popupMenu = new AboutPopup();
            popupMenu.show(this, 0, 0);
        });
        helpMenu.add(about);
        add(helpMenu);
    }
}
