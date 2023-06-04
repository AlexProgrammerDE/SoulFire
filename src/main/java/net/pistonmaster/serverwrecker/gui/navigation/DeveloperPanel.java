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

import javafx.stage.FileChooser;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.gui.libs.JFXFileHelper;
import net.pistonmaster.serverwrecker.logging.LogAppender;
import net.pistonmaster.serverwrecker.settings.DevSettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import org.apache.logging.log4j.Level;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DeveloperPanel extends NavigationItem implements SettingsDuplex<DevSettings> {
    private final JCheckBox debug = new JCheckBox();

    @Inject
    public DeveloperPanel(ServerWrecker serverWrecker, LogAppender logAppender) {
        serverWrecker.getSettingsManager().registerDuplex(DevSettings.class, this);

        setLayout(new GridLayout(0, 2));

        add(new JLabel("Debug:"));
        add(debug);

        add(new JLabel("Save Log:"));
        JButton saveLog = new JButton("Save Log");
        add(saveLog);

        debug.addActionListener(listener -> {
            if (debug.isSelected()) {
                serverWrecker.setupLogging(Level.DEBUG);
            } else {
                serverWrecker.setupLogging(Level.INFO);
            }
        });

        saveLog.addActionListener(listener -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialDirectory(ServerWrecker.DATA_FOLDER.toFile());
            chooser.setTitle("Save Log");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Log Files", "log"));
            Path selectedFile = JFXFileHelper.showSaveDialog(chooser);
            if (selectedFile == null) {
                return;
            }

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
    public String getNavigationId() {
        return "dev-menu";
    }

    @Override
    public void onSettingsChange(DevSettings settings) {
        debug.setSelected(settings.debug());
    }

    @Override
    public DevSettings collectSettings() {
        return new DevSettings(debug.isSelected());
    }
}
