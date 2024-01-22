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
package net.pistonmaster.soulfire.client.gui.navigation;

import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.client.gui.GUIManager;
import net.pistonmaster.soulfire.client.gui.LogPanel;
import net.pistonmaster.soulfire.client.gui.libs.JFXFileHelper;
import net.pistonmaster.soulfire.util.BuiltinSettingsConstants;
import net.pistonmaster.soulfire.util.SWPathConstants;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@Slf4j
public class DeveloperPanel extends NavigationItem {
    @Inject
    public DeveloperPanel(GUIManager guiManager, LogPanel logPanel, CardsContainer cardsContainer) {
        setLayout(new GridLayout(0, 2));

        GeneratedPanel.addComponents(this, cardsContainer.getByNamespace(BuiltinSettingsConstants.DEV_SETTINGS_ID), guiManager.settingsManager());

        add(new JLabel("Save Log"));
        var saveLog = new JButton("Click to save");
        add(saveLog);

        saveLog.addActionListener(listener -> JFXFileHelper.showSaveDialog(SWPathConstants.DATA_FOLDER, Map.of(
                "Log Files", "log"
        ), "log.txt").ifPresent(file -> {
            try (var writer = Files.newBufferedWriter(file)) {
                writer.write(logPanel.messageLogPanel().getLogs());
                log.info("Saved log to: {}", file);
            } catch (IOException e) {
                log.error("Failed to save log!", e);
            }
        }));
    }

    @Override
    public String getNavigationName() {
        return "Developer Tools";
    }

    @Override
    public String getNavigationId() {
        return "dev-menu";
    }
}
