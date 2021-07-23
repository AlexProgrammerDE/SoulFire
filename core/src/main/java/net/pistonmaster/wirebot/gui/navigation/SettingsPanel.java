package net.pistonmaster.wirebot.gui.navigation;

import net.pistonmaster.wirebot.WireBot;
import net.pistonmaster.wirebot.common.GameVersion;
import net.pistonmaster.wirebot.common.Options;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;

public class SettingsPanel extends NavigationItem {
    private final JTextField hostInput;
    private final JTextField portInput;
    private final JSpinner delay;
    private final JCheckBox autoRegister;
    private final JSpinner amount;
    private final JTextField nameFormat;
    private final JComboBox<String> versionBox;

    public SettingsPanel(WireBot botManager) {
        super();

        setLayout(new GridLayout(0, 2));

        add(new JLabel("Host: "));
        hostInput = new JTextField("127.0.0.1");
        add(hostInput);

        add(new JLabel("Port: "));
        portInput = new JTextField("25565");
        add(portInput);

        add(new JLabel("Join delay (ms): "));
        delay = new JSpinner();
        delay.setValue(1000);
        add(delay);

        add(new JLabel("Auto Register: "));
        autoRegister = new JCheckBox();
        add(autoRegister);

        add(new JLabel("Amount: "));
        amount = new JSpinner();
        amount.setValue(20);
        add(amount);

        add(new JLabel("NameFormat: "));
        nameFormat = new JTextField("Bot-%d");
        add(nameFormat);

        versionBox = new JComboBox<>();
        Arrays.stream(GameVersion.values())
                .sorted(Comparator.reverseOrder())
                .map(GameVersion::getVersion)
                .forEach(versionBox::addItem);

        add(versionBox);
    }

    @Override
    public String getNavigationName() {
        return "Settings";
    }

    @Override
    public String getRightPanelContainerConstant() {
        return RightPanelContainer.SETTINGS_MENU;
    }

    public Options generateOptions() {
        return new Options(
                hostInput.getText(),
                Integer.parseInt(portInput.getText()),
                (int) amount.getValue(),
                (int) delay.getValue(),
                nameFormat.getText(),
                GameVersion.findByName((String) versionBox.getSelectedItem()),
                autoRegister.isSelected(),
                DeveloperPanel.debug.isSelected());
    }
}
