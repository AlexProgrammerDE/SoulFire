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
package net.pistonmaster.serverwrecker.gui.navigation;

import ch.qos.logback.classic.Level;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.state.AttackEndEvent;
import net.pistonmaster.serverwrecker.api.event.state.AttackStartEvent;
import net.pistonmaster.serverwrecker.logging.LogAppender;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DeveloperPanel extends NavigationItem {
    public static final JCheckBox debug = new JCheckBox();
    public static final JButton saveLog = new JButton("Save Log");

    @Inject
    public DeveloperPanel(ServerWrecker serverWrecker, LogAppender logAppender) {
        super();

        setLayout(new GridLayout(0, 2));

        add(new JLabel("Debug: "));
        add(debug);

        add(new JLabel("Save Log: "));
        add(saveLog);

        ServerWreckerAPI.registerListener(AttackStartEvent.class, event ->
                debug.setEnabled(false));
        ServerWreckerAPI.registerListener(AttackEndEvent.class, event ->
                debug.setEnabled(true));

        debug.addActionListener(listener -> {
            if (debug.isSelected()) {
                serverWrecker.setupLogging(Level.DEBUG);
            } else {
                serverWrecker.setupLogging(Level.INFO);
            }
        });

        saveLog.addActionListener(listener -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Log");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().endsWith(".log");
                }

                @Override
                public String getDescription() {
                    return "Log Files";
                }
            });

            int result = fileChooser.showSaveDialog(null);

            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            Path selectedFile = fileChooser.getSelectedFile().toPath();

            try (BufferedWriter writer = Files.newBufferedWriter(selectedFile)) {
                logAppender.getLogLines().forEach(entry -> {
                    try {
                        writer.write(entry);
                        writer.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public String getNavigationName() {
        return "Developer Tools";
    }

    @Override
    public String getRightPanelContainerConstant() {
        return RightPanelContainer.DEV_MENU;
    }
}
