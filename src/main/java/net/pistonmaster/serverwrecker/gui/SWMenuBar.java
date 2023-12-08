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
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.*;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicIJTheme;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import javafx.stage.FileChooser;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.gui.WindowCloseEvent;
import net.pistonmaster.serverwrecker.gui.libs.JFXFileHelper;
import net.pistonmaster.serverwrecker.gui.popups.AboutPopup;
import net.pistonmaster.serverwrecker.util.SWPathConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SWMenuBar extends JMenuBar {
    private static final Logger LOGGER = LoggerFactory.getLogger(SWMenuBar.class);
    private static final List<Class<? extends BasicLookAndFeel>> THEMES;

    static {
        var tempThemes = new ArrayList<>(List.of(
                FlatDarculaLaf.class,
                FlatIntelliJLaf.class,
                FlatDarkLaf.class,
                FlatLightLaf.class,
                FlatMacDarkLaf.class,
                FlatMacLightLaf.class,
                FlatOneDarkIJTheme.class,
                FlatArcOrangeIJTheme.class,
                FlatArcDarkOrangeIJTheme.class,
                FlatCyanLightIJTheme.class,
                FlatDarkPurpleIJTheme.class,
                FlatMaterialDarkerIJTheme.class,
                FlatMaterialOceanicIJTheme.class,
                FlatCarbonIJTheme.class
        ));
        THEMES = List.copyOf(tempThemes);
    }

    @Inject
    public SWMenuBar(GUIManager guiManager) {
        var fileMenu = new JMenu("File");
        var loadProfile = new JMenuItem("Load Profile");
        loadProfile.addActionListener(e -> {
            var chooser = new FileChooser();
            chooser.setInitialDirectory(SWPathConstants.PROFILES_FOLDER.toFile());
            chooser.setTitle("Load Profile");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ServerWrecker profile", "*.json"));
            JFXFileHelper.showOpenDialog(chooser).thenAcceptAsync(file -> {
                if (file == null) {
                    return;
                }

                try {
                    guiManager.getSettingsManager().loadProfile(file);
                    LOGGER.info("Loaded profile!");
                } catch (IOException ex) {
                    LOGGER.warn("Failed to load profile!", ex);
                }
            }, guiManager.getThreadPool());
        });

        fileMenu.add(loadProfile);
        var saveProfile = new JMenuItem("Save Profile");
        saveProfile.addActionListener(e -> {
            var chooser = new FileChooser();
            chooser.setInitialDirectory(SWPathConstants.PROFILES_FOLDER.toFile());
            chooser.setTitle("Save Profile");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ServerWrecker profile", "*.json"));
            JFXFileHelper.showSaveDialog(chooser).thenAcceptAsync(file -> {
                if (file == null) {
                    return;
                }

                // Add .json if not present
                var path = file.toString();
                if (!path.endsWith(".json")) {
                    path += ".json";
                }

                try {
                    guiManager.getSettingsManager().saveProfile(Path.of(path));
                    LOGGER.info("Saved profile!");
                } catch (IOException ex) {
                    LOGGER.warn("Failed to save profile!", ex);
                }
            }, guiManager.getThreadPool());
        });

        fileMenu.add(saveProfile);

        fileMenu.addSeparator();

        var exit = new JMenuItem("Exit");
        exit.addActionListener(e -> guiManager.shutdown());
        fileMenu.add(exit);
        add(fileMenu);

        var window = new JMenu("Options");
        var themeSelector = new JMenu("Theme");
        for (var theme : THEMES) {
            var themeItem = new JMenuItem(theme.getSimpleName());
            themeItem.addActionListener(e -> {
                GUIClientProps.setString("theme", theme.getName());
                SwingUtilities.invokeLater(ThemeUtil::setLookAndFeel);
            });
            themeSelector.add(themeItem);
        }
        window.add(themeSelector);
        add(window);

        var helpMenu = new JMenu("Help");
        var about = new JMenuItem("About");
        about.addActionListener(e -> showAboutDialog());
        helpMenu.add(about);
        add(helpMenu);

        var desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
            desktop.setAboutHandler(e -> showAboutDialog());
        }

        if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
            desktop.setQuitHandler((e, response) -> {
                var event = new WindowCloseEvent();
                ServerWreckerAPI.postEvent(event);
                var canQuit = !event.isCancelled();
                if (canQuit) {
                    response.performQuit();
                } else {
                    response.cancelQuit();
                }
            });
        }
    }

    private void showAboutDialog() {
        JPopupMenu popupMenu = new AboutPopup();
        popupMenu.show(this, 0, 0);
    }
}
