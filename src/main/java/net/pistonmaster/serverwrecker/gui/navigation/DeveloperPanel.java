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
import net.pistonmaster.serverwrecker.ServerWreckerBootstrap;
import net.pistonmaster.serverwrecker.gui.GUIManager;
import net.pistonmaster.serverwrecker.gui.LogPanel;
import net.pistonmaster.serverwrecker.gui.libs.JFXFileHelper;
import net.pistonmaster.serverwrecker.settings.DevSettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;

public class DeveloperPanel extends NavigationItem implements SettingsDuplex<DevSettings> {
    private final JCheckBox viaDebug = new JCheckBox();
    private final JCheckBox nettyDebug = new JCheckBox();
    private final JCheckBox gRPCDebug = new JCheckBox();
    private final JCheckBox coreDebug = new JCheckBox();

    @Inject
    public DeveloperPanel(GUIManager guiManager, LogPanel logPanel) {
        guiManager.getSettingsManager().registerDuplex(DevSettings.class, this);

        setLayout(new GridLayout(0, 2));

        add(new JLabel("Via Debug:"));
        add(viaDebug);

        add(new JLabel("Netty Debug:"));
        add(nettyDebug);

        add(new JLabel("gRPC Debug:"));
        add(gRPCDebug);

        add(new JLabel("Core Debug:"));
        add(coreDebug);

        add(new JLabel("Save Log:"));
        var saveLog = new JButton("Save Log");
        add(saveLog);

        saveLog.addActionListener(listener -> {
            var chooser = new FileChooser();
            chooser.setInitialDirectory(ServerWreckerBootstrap.DATA_FOLDER.toFile());
            chooser.setTitle("Save Log");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Log Files", "*.log"));
            JFXFileHelper.showSaveDialog(chooser).thenAcceptAsync(file -> {
                if (file == null) {
                    return;
                }

                try (var writer = Files.newBufferedWriter(file)) {
                    writer.write(logPanel.getMessageLogPanel().getLogs());
                    guiManager.getLogger().info("Saved log to: {}", file);
                } catch (IOException e) {
                    guiManager.getLogger().error("Failed to save log!", e);
                }
            }, guiManager.getThreadPool());
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
        viaDebug.setSelected(settings.viaDebug());
        nettyDebug.setSelected(settings.nettyDebug());
        gRPCDebug.setSelected(settings.grpcDebug());
        coreDebug.setSelected(settings.coreDebug());
    }

    @Override
    public DevSettings collectSettings() {
        return new DevSettings(
                viaDebug.isSelected(),
                nettyDebug.isSelected(),
                gRPCDebug.isSelected(),
                coreDebug.isSelected()
        );
    }
}
