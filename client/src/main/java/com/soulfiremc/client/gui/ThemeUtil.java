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
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.util.SystemInfo;
import com.soulfiremc.client.gui.libs.TerminalTheme;
import java.util.stream.Stream;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.basic.BasicLookAndFeel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThemeUtil {
  private ThemeUtil() {}

  public static String getThemeClassName() {
    return GUIClientProps.getString("theme", FlatDarculaLaf.class.getName());
  }

  public static TerminalTheme getTerminal() {
    var current = GUIClientProps.getString("terminal", TerminalTheme.THEMES[0].name());

    return Stream.of(TerminalTheme.THEMES)
      .filter(theme -> theme.name().equals(current))
      .findFirst()
      .orElse(TerminalTheme.THEMES[0]);
  }

  /**
   * Apply the current theme that is set in the settings. This will also save the theme to the
   * settings. You need to invoke SwingUtilities.updateComponentTreeUI(frame); after this method.
   */
  public static void setLookAndFeel() {
    var themeSettings = getThemeClassName();
    if (themeSettings.equals(UIManager.getLookAndFeel().getClass().getName())) {
      return;
    }

    try {
      var theme =
        Class.forName(themeSettings)
          .asSubclass(BasicLookAndFeel.class)
          .getDeclaredConstructor()
          .newInstance();

      FlatAnimatedLafChange.showSnapshot();

      UIManager.setLookAndFeel(theme);

      FlatLaf.updateUI();

      FlatAnimatedLafChange.hideSnapshotWithAnimation();
    } catch (UnsupportedLookAndFeelException | ReflectiveOperationException e) {
      log.error("Failed to set theme!", e);
    }
  }

  public static void initFlatLaf() {
    // Needs to be before the other stuff to work properly
    if (SystemInfo.isMacOS) {
      // Use top screen menu bar on macOS
      System.setProperty("apple.laf.useScreenMenuBar", "true");

      // Set name in top menu bar
      System.setProperty("apple.awt.application.name", "SoulFire");

      // Color the frame
      System.setProperty("apple.awt.application.appearance", "system");
    } else if (SystemInfo.isLinux) {
      // Make window decorations like on windows
      JFrame.setDefaultLookAndFeelDecorated(true);
      JDialog.setDefaultLookAndFeelDecorated(true);
    }

    FlatInterFont.install();
    FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
    FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
    FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);

    FlatJetBrainsMonoFont.install();
    FlatLaf.setPreferredMonospacedFontFamily(FlatJetBrainsMonoFont.FAMILY);

    FlatInspector.install("ctrl shift I");
    FlatUIDefaultsInspector.install("ctrl shift O");
    ToolTipManager.sharedInstance().setInitialDelay(100);
    ToolTipManager.sharedInstance().setDismissDelay(10_000);
    UIManager.put("PasswordField.showRevealButton", true);
  }
}
