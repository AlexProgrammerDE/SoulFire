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

import net.pistonmaster.serverwrecker.AttackManager;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.common.AttackState;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {
    @Inject
    public ControlPanel(ServerWrecker serverWrecker) {
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

                    serverWrecker.startAttack();
                } catch (Exception ex) {
                    serverWrecker.getLogger().info(ex.getMessage(), ex);
                }
            });
        });

        pauseButton.addActionListener(action -> {
            AttackManager attackManager = serverWrecker.getAttacks().stream().findFirst().orElse(null);

            if (attackManager == null) {
                return;
            }

            if (attackManager.getAttackState().isRunning()) {
                attackManager.setAttackState(AttackState.PAUSED);
            } else if (attackManager.getAttackState().isPaused()) {
                attackManager.setAttackState(AttackState.RUNNING);
            } else {
                throw new IllegalStateException("Attack state is not running or paused!");
            }

            if (attackManager.getAttackState().isPaused()) {
                serverWrecker.getLogger().info("Paused bot attack");
                pauseButton.setText("Resume");
            } else {
                serverWrecker.getLogger().info("Resumed bot attack");
                pauseButton.setText("Pause");
            }
        });

        stopButton.addActionListener(action -> {
            AttackManager attackManager = serverWrecker.getAttacks().stream().findFirst().orElse(null);

            if (attackManager == null) {
                return;
            }

            startButton.setEnabled(true);

            pauseButton.setEnabled(false);
            pauseButton.setText("Pause");
            attackManager.setAttackState(AttackState.PAUSED);

            stopButton.setEnabled(false);

            serverWrecker.stopAllAttacks();
        });
    }
}
