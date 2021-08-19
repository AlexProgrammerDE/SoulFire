package net.pistonmaster.serverwrecker.gui.navigation;

import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.common.Options;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {
    public ControlPanel(RightPanelContainer container, ServerWrecker wireBot) {
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

            wireBot.getThreadPool().submit(() -> {
                try {
                    startButton.setEnabled(false);

                    pauseButton.setEnabled(true);
                    pauseButton.setText("Pause");
                    wireBot.setPaused(false);

                    stopButton.setEnabled(true);

                    ServerWrecker.getLogger().info("Preparing bot attack at {}", options.hostname);
                    wireBot.start(options);
                } catch (Exception ex) {
                    ServerWrecker.getLogger().info(ex.getMessage(), ex);
                }
            });
        });

        pauseButton.addActionListener(action -> {
            wireBot.setPaused(!wireBot.isPaused());
            if (wireBot.isPaused()) {
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
            wireBot.setPaused(false);

            stopButton.setEnabled(false);

            ServerWrecker.getLogger().info("Stopping bot attack");
            wireBot.stop();
        });
    }
}
