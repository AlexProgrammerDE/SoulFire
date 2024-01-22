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

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.*;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicIJTheme;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.client.gui.libs.JFXFileHelper;
import net.pistonmaster.soulfire.client.gui.popups.AboutPopup;
import net.pistonmaster.soulfire.server.api.SoulFireAPI;
import net.pistonmaster.soulfire.server.api.event.gui.WindowCloseEvent;
import net.pistonmaster.soulfire.util.SWPathConstants;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class SWMenuBar extends JMenuBar {
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

    private final GUIFrame guiFrame;

    @Inject
    public SWMenuBar(GUIManager guiManager, GUIFrame guiFrame) {
        this.guiFrame = guiFrame;

        var fileMenu = new JMenu("File");
        var loadProfile = new JMenuItem("Load Profile");
        loadProfile.addActionListener(e -> JFXFileHelper.showOpenDialog(SWPathConstants.PROFILES_FOLDER, Map.of(
                "SoulFire profile", "json"
        )).ifPresent(file -> {
            try {
                guiManager.settingsManager().loadProfile(file);
                log.info("Loaded profile!");
            } catch (IOException ex) {
                log.warn("Failed to load profile!", ex);
            }
        }));

        fileMenu.add(loadProfile);
        var saveProfile = new JMenuItem("Save Profile");
        saveProfile.addActionListener(e -> JFXFileHelper.showSaveDialog(SWPathConstants.PROFILES_FOLDER, Map.of(
                "SoulFire profile", "json"
        ), "profile.json").ifPresent(file -> {
            // Add .json if not present
            var path = file.toString();
            if (!path.endsWith(".json")) {
                path += ".json";
            }

            try {
                guiManager.settingsManager().saveProfile(Path.of(path));
                log.info("Saved profile!");
            } catch (IOException ex) {
                log.warn("Failed to save profile!", ex);
            }
        }));

        fileMenu.add(saveProfile);

        fileMenu.addSeparator();

        var exit = new JMenuItem("Exit");
        exit.addActionListener(e -> guiManager.shutdown());
        fileMenu.add(exit);
        add(fileMenu);

        var viewMenu = new JMenu("View");
        var themeSelector = new JMenu("Theme");
        for (var theme : THEMES) {
            var themeItem = new JMenuItem(theme.getSimpleName());
            themeItem.addActionListener(e -> {
                GUIClientProps.setString("theme", theme.getName());
                SwingUtilities.invokeLater(ThemeUtil::setLookAndFeel);
            });
            themeSelector.add(themeItem);
        }
        viewMenu.add(themeSelector);

        /*
        viewMenu.addSeparator();

        var windowMenu = new JMenu("Window");
        var trafficGraph = new JMenuItem("Traffic Monitor");
        trafficGraph.addActionListener(e -> {
            System.out.println("TODO: Open traffic graph");
        });
        windowMenu.add(trafficGraph);
        viewMenu.add(windowMenu);
         */
        add(viewMenu);

        var helpMenu = new JMenu("Help");
        var openHome = new JMenuItem("Show home");
        openHome.addActionListener(e -> openHome());
        helpMenu.add(openHome);

        helpMenu.addSeparator();

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
                SoulFireAPI.postEvent(event);
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
        new AboutPopup(guiFrame);
    }

    private void openHome() {
        try {
            Desktop.getDesktop().browse(SWPathConstants.DATA_FOLDER.toUri());
        } catch (IOException e) {
            log.warn("Failed to open home page!", e);
        }
    }
}
