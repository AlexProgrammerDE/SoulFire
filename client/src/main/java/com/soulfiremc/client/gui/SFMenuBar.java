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

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicIJTheme;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.util.SystemInfo;
import com.soulfiremc.client.gui.libs.JFXFileHelper;
import com.soulfiremc.client.gui.libs.TerminalTheme;
import com.soulfiremc.client.gui.popups.AboutPopup;
import com.soulfiremc.util.SFPathConstants;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicLookAndFeel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SFMenuBar extends JMenuBar {
  private static final List<Class<? extends BasicLookAndFeel>> THEMES;

  static {
    var tempThemes =
      new ArrayList<>(
        List.of(
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
          FlatCarbonIJTheme.class));
    THEMES = List.copyOf(tempThemes);
  }

  private final GUIManager guiManager;
  private final GUIFrame guiFrame;

  @Inject
  public SFMenuBar(GUIManager guiManager, LogPanel logPanel, GUIFrame guiFrame) {
    this.guiManager = guiManager;
    this.guiFrame = guiFrame;

    var fileMenu = new JMenu("File");
    var loadProfile = new JMenu("Load Profile");
    try {
      Files.list(SFPathConstants.PROFILES_DIRECTORY)
        .filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".json"))
        .forEach(
          profile -> {
            var profileItem = new JMenuItem(profile.getFileName().toString());
            profileItem.addActionListener(
              e -> {
                try {
                  guiManager.clientSettingsManager().loadProfile(profile);
                  log.info("Loaded profile: {}", profile.getFileName());
                } catch (IOException ex) {
                  log.warn("Failed to load profile: {}", profile.getFileName(), ex);
                }
              });
            loadProfile.add(profileItem);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (loadProfile.getItemCount() > 0) {
      loadProfile.addSeparator();
    }

    var fromFile = new JMenuItem("From File");
    fromFile.addActionListener(
      e ->
        JFXFileHelper.showOpenDialog(
            SFPathConstants.PROFILES_DIRECTORY, Map.of("SoulFire profile", "json"))
          .ifPresent(
            file -> {
              try {
                guiManager.clientSettingsManager().loadProfile(file);
                log.info("Loaded profile!");
              } catch (IOException ex) {
                log.warn("Failed to load profile!", ex);
              }
            }));
    loadProfile.add(fromFile);

    fileMenu.add(loadProfile);
    var saveProfile = new JMenuItem("Save Profile");
    saveProfile.addActionListener(
      e ->
        JFXFileHelper.showSaveDialog(
            SFPathConstants.PROFILES_DIRECTORY,
            Map.of("SoulFire profile", "json"),
            "profile.json")
          .ifPresent(
            file -> {
              // Add .json if not present
              var path = file.toString();
              if (!path.endsWith(".json")) {
                path += ".json";
              }

              try {
                guiManager.clientSettingsManager().saveProfile(Path.of(path));
                log.info("Saved profile!");
              } catch (IOException ex) {
                log.warn("Failed to save profile!", ex);
              }
            }));

    fileMenu.add(saveProfile);

    if (!SystemInfo.isMacOS) {
      fileMenu.addSeparator();

      var exit = new JMenuItem("Exit");
      exit.addActionListener(e -> guiManager.shutdown());
      fileMenu.add(exit);
    }

    add(fileMenu);

    var viewMenu = new JMenu("View");
    var themeSelector = new JMenu("Theme");
    var themeUpdateCallbacks = new ArrayList<Runnable>();
    for (var theme : THEMES) {
      var themeItem = new JRadioButtonMenuItem(theme.getSimpleName());
      themeUpdateCallbacks.add(
        () -> themeItem.setSelected(theme.getName().equals(ThemeUtil.getThemeClassName())));
      themeItem.addActionListener(
        e -> {
          GUIClientProps.setString("theme", theme.getName());
          SwingUtilities.invokeLater(ThemeUtil::setLookAndFeel);
          themeUpdateCallbacks.forEach(Runnable::run);
          log.info("Changed theme to: {}", theme.getName());
        });
      themeSelector.add(themeItem);
    }
    themeUpdateCallbacks.forEach(Runnable::run);
    viewMenu.add(themeSelector);

    var terminalSelector = new JMenu("Terminal");
    var terminalUpdateCallbacks = new ArrayList<Runnable>();
    for (var terminal : TerminalTheme.THEMES) {
      var terminalItem = new JRadioButtonMenuItem(terminal.name());
      terminalUpdateCallbacks.add(
        () -> terminalItem.setSelected(terminal == ThemeUtil.getTerminal()));
      terminalItem.addActionListener(
        e -> {
          GUIClientProps.setString("terminal", terminal.name());
          logPanel.messageLogPanel().refreshTheme();
          terminalUpdateCallbacks.forEach(Runnable::run);
          log.info(
            "Changed terminal to: {} (You may need to restart for the text color to look right)",
            terminal.name());
        });
      terminalSelector.add(terminalItem);
    }
    terminalUpdateCallbacks.forEach(Runnable::run);
    viewMenu.add(terminalSelector);

    /*
    viewMenu.addSeparator();

    var windowMenu = new JMenu("Window");
    var trafficGraph = new JMenuItem("Traffic Monitor");
    trafficGraph.addActionListener(e -> {
        log.info("TODO: Open traffic graph");
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

    var saveLogs = new JMenuItem("Save logs");
    saveLogs.addActionListener(
      listener ->
        JFXFileHelper.showSaveDialog(
            SFPathConstants.CLIENT_DATA_DIRECTORY, Map.of("Log Files", "log"), "log.txt")
          .ifPresent(
            file -> {
              try {
                Files.writeString(file, logPanel.messageLogPanel().getLogs());
                log.info("Saved log to: {}", file);
              } catch (IOException e) {
                log.error("Failed to save log!", e);
              }
            }));
    helpMenu.add(saveLogs);

    if (!SystemInfo.isMacOS) {
      helpMenu.addSeparator();

      var about = new JMenuItem("About");
      about.addActionListener(e -> showAboutDialog());
      helpMenu.add(about);
    }

    add(helpMenu);

    var desktop = Desktop.getDesktop();
    if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
      desktop.setAboutHandler(e -> showAboutDialog());
    }

    if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
      desktop.setQuitHandler((e, response) -> response.performQuit());
    }
  }

  private void showAboutDialog() {
    new AboutPopup(guiFrame);
  }

  private void openHome() {
    guiManager.browse(SFPathConstants.CLIENT_DATA_DIRECTORY.toUri());
  }
}
