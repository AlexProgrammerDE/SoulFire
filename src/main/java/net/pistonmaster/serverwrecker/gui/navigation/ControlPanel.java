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

import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.common.AttackState;
import net.pistonmaster.serverwrecker.common.SWOptions;

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
            serverWrecker.getThreadPool().submit(() -> {
                try {
                    startButton.setEnabled(false);

                    pauseButton.setEnabled(true);
                    pauseButton.setText("Pause");

                    stopButton.setEnabled(true);

                    serverWrecker.start();
                } catch (Exception ex) {
                    serverWrecker.getLogger().info(ex.getMessage(), ex);
                }
            });
        });

        pauseButton.addActionListener(action -> {
            if (serverWrecker.getAttackState().isRunning()) {
                serverWrecker.setAttackState(AttackState.PAUSED);
            } else if (serverWrecker.getAttackState().isPaused()) {
                serverWrecker.setAttackState(AttackState.RUNNING);
            } else {
                throw new IllegalStateException("Attack state is not running or paused!");
            }

            if (serverWrecker.getAttackState().isPaused()) {
                serverWrecker.getLogger().info("Paused bot attack");
                pauseButton.setText("Resume");
            } else {
                serverWrecker.getLogger().info("Resumed bot attack");
                pauseButton.setText("Pause");
            }
        });

        stopButton.addActionListener(action -> {
            startButton.setEnabled(true);

            pauseButton.setEnabled(false);
            pauseButton.setText("Pause");
            serverWrecker.setAttackState(AttackState.PAUSED);

            stopButton.setEnabled(false);

            serverWrecker.getLogger().info("Stopping bot attack");
            serverWrecker.stop();
        });
    }
}
