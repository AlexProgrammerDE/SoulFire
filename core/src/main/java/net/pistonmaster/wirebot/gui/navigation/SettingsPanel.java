package net.pistonmaster.wirebot.gui.navigation;

import net.pistonmaster.wirebot.WireBot;
import net.pistonmaster.wirebot.common.GameVersion;
import net.pistonmaster.wirebot.common.Options;
import net.pistonmaster.wirebot.gui.LoadAccountsListener;
import net.pistonmaster.wirebot.gui.LoadProxiesListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;

public class SettingsPanel extends NavigationItem {
    private final WireBot botManager;

    public SettingsPanel(WireBot botManager) {
        super();
        this.botManager = botManager;

        setLayout(new GridLayout(0, 2));

        add(new JLabel("Host: "));
        JTextField hostInput = new JTextField("127.0.0.1");
        add(hostInput);

        add(new JLabel("Port: "));
        JTextField portInput = new JTextField("25565");
        add(portInput);

        add(new JLabel("Join delay (ms): "));
        JSpinner delay = new JSpinner();
        delay.setValue(1000);
        add(delay);

        add(new JLabel("Auto Register: "));
        JCheckBox autoRegister = new JCheckBox();
        add(autoRegister);

        add(new JLabel("Amount: "));
        JSpinner amount = new JSpinner();
        amount.setValue(20);
        add(amount);

        add(new JLabel("NameFormat: "));
        JTextField nameFormat = new JTextField("Bot-%d");
        add(nameFormat);

        JComboBox<String> versionBox = new JComboBox<>();
        Arrays.stream(GameVersion.values())
                .sorted(Comparator.reverseOrder())
                .map(GameVersion::getVersion)
                .forEach(versionBox::addItem);

        add(versionBox);

        JPanel startStopPanel = new JPanel();
        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        startStopPanel.add(startButton);
        startStopPanel.add(stopButton);
        add(startStopPanel);



        startButton.addActionListener(action -> {
            // collect the options on the gui thread
            // for thread-safety
            Options options = new Options(
                    hostInput.getText(),
                    Integer.parseInt(portInput.getText()),
                    (int) amount.getValue(),
                    (int) delay.getValue(),
                    nameFormat.getText(),
                    GameVersion.findByName((String) versionBox.getSelectedItem()),
                    autoRegister.isSelected());

            botManager.getThreadPool().submit(() -> {
                try {
                    botManager.start(options);
                } catch (Exception ex) {
                    WireBot.getLogger().log(Level.INFO, ex.getMessage(), ex);
                }
            });
        });

        stopButton.addActionListener(action -> botManager.stop());
    }

    @Override
    public String getNavigationName() {
        return "Settings";
    }

    @Override
    public String getRightPanelContainerConstant() {
        return RightPanelContainer.SETTINGS_MENU;
    }
}
