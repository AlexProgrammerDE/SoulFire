/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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

import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.common.Options;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {
    @Inject
    public ControlPanel(RightPanelContainer container, ServerWrecker serverWrecker) {
        JButton startButton = new JButton("Start");
        JButton pauseButton = new JButton("Pause");
        JButton stopButton = new JButton("Stop");

        startButton.setSelected(true);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        setLayout(new GridLayout(3, 3));
        add(startButton);
        add(pauseButton);
        add(stopButton);

        startButton.addActionListener(action -> {
            Options options = container.getPanel(SettingsPanel.class).generateOptions();

            serverWrecker.getThreadPool().submit(() -> {
                try {
                    startButton.setEnabled(false);

                    pauseButton.setEnabled(true);
                    pauseButton.setText("Pause");
                    serverWrecker.setPaused(false);

                    stopButton.setEnabled(true);

                    ServerWrecker.getLogger().info("Preparing bot attack at {}", options.hostname());
                    serverWrecker.start(options);
                } catch (Exception ex) {
                    ServerWrecker.getLogger().info(ex.getMessage(), ex);
                }
            });
        });

        pauseButton.addActionListener(action -> {
            serverWrecker.setPaused(!serverWrecker.isPaused());
            if (serverWrecker.isPaused()) {
                ServerWrecker.getLogger().info("Paused bot attack");
                pauseButton.setText("Resume");
            } else {
                ServerWrecker.getLogger().info("Resumed bot attack");
                pauseButton.setText("Pause");
            }
        });

        stopButton.addActionListener(action -> {
            startButton.setEnabled(true);

            pauseButton.setEnabled(false);
            pauseButton.setText("Pause");
            serverWrecker.setPaused(false);

            stopButton.setEnabled(false);

            ServerWrecker.getLogger().info("Stopping bot attack");
            serverWrecker.stop();
        });
    }
}
