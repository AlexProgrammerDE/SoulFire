package net.pistonmaster.wirebot.gui.navigation;

import net.pistonmaster.wirebot.WireBot;
import net.pistonmaster.wirebot.common.Options;

import javax.swing.*;
import java.util.logging.Level;

public class ControlPanel extends NavigationItem {
    public ControlPanel(RightPanelContainer container, WireBot wireBot) {
        JPanel startStopPanel = new JPanel();
        JButton startButton = new JButton("Start");
        JButton pauseButton = new JButton("Pause");
        JButton stopButton = new JButton("Stop");

        startButton.setSelected(true);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        startStopPanel.add(startButton);
        startStopPanel.add(pauseButton);
        startStopPanel.add(stopButton);

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

        add(startStopPanel);
    }

    @Override
    public String getNavigationName() {
        return "Controls";
    }

    @Override
    public String getRightPanelContainerConstant() {
        return RightPanelContainer.CONTROL_MENU;
    }
}
