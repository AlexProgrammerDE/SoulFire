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

import li.flor.nativejfilechooser.NativeJFileChooser;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.gui.popups.AboutPopup;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Path;

public class SWMenuBar extends JMenuBar {
    @Inject
    public SWMenuBar(ServerWrecker serverWrecker) {
        JMenu fileMenu = new JMenu("File");
        JMenuItem loadProfile = new JMenuItem("Load Profile");
        loadProfile.addActionListener(e -> {
            JFileChooser chooser = new NativeJFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setDialogTitle("Load Profile");
            chooser.setApproveButtonText("Load");
            chooser.addChoosableFileFilter(new FileNameExtensionFilter("ServerWrecker profile", "json"));
            chooser.setMultiSelectionEnabled(false);
            chooser.showOpenDialog(this);

            if (chooser.getSelectedFile() != null) {
                serverWrecker.getSettingsManager().loadProfile(chooser.getSelectedFile().toPath());
            }
        });

        fileMenu.add(loadProfile);
        JMenuItem saveProfile = new JMenuItem("Save Profile");
        saveProfile.addActionListener(e -> {
            JFileChooser chooser = new NativeJFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setDialogTitle("Save Profile");
            chooser.setApproveButtonText("Save");
            chooser.addChoosableFileFilter(new FileNameExtensionFilter("ServerWrecker profile", "json"));
            chooser.setMultiSelectionEnabled(false);
            chooser.showSaveDialog(this);

            if (chooser.getSelectedFile() != null) {
                // Add .json if not present
                String path = chooser.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".json")) {
                    path += ".json";
                }

                serverWrecker.getSettingsManager().saveProfile(Path.of(path));
            }
        });
        fileMenu.add(saveProfile);

        fileMenu.addSeparator();

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> serverWrecker.shutdown(true));
        fileMenu.add(exit);
        add(fileMenu);

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
