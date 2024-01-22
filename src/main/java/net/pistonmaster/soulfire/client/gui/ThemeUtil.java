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
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector;
import com.formdev.flatlaf.util.SystemInfo;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;

@Slf4j
public class ThemeUtil {
    private ThemeUtil() {
    }

    /**
     * Apply the current theme that is set in the settings.
     * This will also save the theme to the settings.
     * You need to invoke SwingUtilities.updateComponentTreeUI(frame); after this method.
     */
    public static void setLookAndFeel() {
        var themeSettings = GUIClientProps.getString("theme", FlatDarculaLaf.class.getName());
        if (themeSettings.equals(UIManager.getLookAndFeel().getClass().getName())) {
            return;
        }

        try {
            var theme = Class.forName(themeSettings)
                    .asSubclass(BasicLookAndFeel.class).getDeclaredConstructor().newInstance();

            FlatAnimatedLafChange.showSnapshot();

            UIManager.setLookAndFeel(theme);

            FlatLaf.updateUI();

            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        } catch (UnsupportedLookAndFeelException | ReflectiveOperationException e) {
            log.error("Failed to set theme!", e);
        }
    }

    public static void initFlatLaf() {
        FlatInspector.install("ctrl shift I");
        FlatUIDefaultsInspector.install("ctrl shift O");
        ToolTipManager.sharedInstance().setInitialDelay(100);
        ToolTipManager.sharedInstance().setDismissDelay(10_000);
        UIManager.put("PasswordField.showRevealButton", true);

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
    }
}
