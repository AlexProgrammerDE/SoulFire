package net.pistonmaster.wirebot.gui.navigation;

import net.pistonmaster.wirebot.WireBot;
import net.pistonmaster.wirebot.common.Options;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

public class ControlPanel extends JPanel {
    public ControlPanel(RightPanelContainer container, WireBot wireBot) {
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

                    wireBot.start(options);
                } catch (Exception ex) {
                    WireBot.getLogger().log(Level.INFO, ex.getMessage(), ex);
                }
            });
        });

        pauseButton.addActionListener(action -> {
            wireBot.setPaused(!wireBot.isPaused());
            if (wireBot.isPaused()) {
                pauseButton.setText("Resume");
            } else {
                pauseButton.setText("Pause");
            }
        });

        stopButton.addActionListener(action -> {
            startButton.setEnabled(true);

            pauseButton.setEnabled(false);
            pauseButton.setText("Pause");
            wireBot.setPaused(false);

            stopButton.setEnabled(false);

            wireBot.stop();
        });
    }
}
