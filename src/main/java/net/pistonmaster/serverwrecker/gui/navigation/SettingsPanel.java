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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.pistonmaster.serverwrecker.SWConstants;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.settings.BotSettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettingsPanel extends NavigationItem implements SettingsDuplex<BotSettings> {
    private final JTextField hostInput;
    private final JTextField portInput;
    private final JCheckBox trySrv;
    private final JSpinner joinDelayMs;
    private final JCheckBox waitEstablished;
    private final JSpinner amount;
    private final JComboBox<ProtocolVersion> versionBox;
    private final JSpinner readTimeout;
    private final JSpinner writeTimeout;
    private final JSpinner connectTimeout;

    @Inject
    public SettingsPanel(ServerWrecker serverWrecker) {
        serverWrecker.getSettingsManager().registerDuplex(BotSettings.class, this);

        setLayout(new GridLayout(0, 2));

        add(new JLabel("Host: "));
        hostInput = new JTextField(BotSettings.DEFAULT_HOST);
        add(hostInput);

        add(new JLabel("Port: "));
        portInput = new JTextField(String.valueOf(BotSettings.DEFAULT_PORT));
        add(portInput);

        add(new JLabel("Try SRV record resolving: "));
        trySrv = new PresetJCheckBox(BotSettings.DEFAULT_TRY_SRV);
        add(trySrv);

        add(new JLabel("Join delay (ms): "));
        joinDelayMs = new JSpinner();
        joinDelayMs.setValue(BotSettings.DEFAULT_JOIN_DELAY_MS);
        add(joinDelayMs);

        add(new JLabel("Wait established: "));
        waitEstablished = new PresetJCheckBox(BotSettings.DEFAULT_WAIT_ESTABLISHED);
        add(waitEstablished);

        add(new JLabel("Amount: "));
        amount = new JSpinner();
        amount.setValue(BotSettings.DEFAULT_AMOUNT);
        add(amount);

        add(new JLabel("Version: "));
        versionBox = new JComboBox<>();
        registerVersions();
        add(versionBox);

        add(new JLabel("Read Timeout: "));
        readTimeout = new JSpinner();
        readTimeout.setValue(BotSettings.DEFAULT_READ_TIMEOUT);
        add(readTimeout);

        add(new JLabel("Write Timeout: "));
        writeTimeout = new JSpinner();
        writeTimeout.setValue(BotSettings.DEFAULT_WRITE_TIMEOUT);
        add(writeTimeout);

        add(new JLabel("Connect Timeout: "));
        connectTimeout = new JSpinner();
        connectTimeout.setValue(BotSettings.DEFAULT_CONNECT_TIMEOUT);
        add(connectTimeout);
    }

    public void registerVersions() {
        versionBox.removeAllItems();
        List<ProtocolVersion> versions = new ArrayList<>(SWConstants.getVersionsSorted());
        Collections.reverse(versions);
        versions.forEach(versionBox::addItem);
        versionBox.setSelectedItem(BotSettings.DEFAULT_PROTOCOL_VERSION);
    }

    @Override
    public String getNavigationName() {
        return "Settings";
    }

    @Override
    public String getNavigationId() {
        return "settings-menu";
    }

    @Override
    public void onSettingsChange(BotSettings settings) {
        hostInput.setText(settings.host());
        portInput.setText(String.valueOf(settings.port()));
        amount.setValue(settings.amount());
        joinDelayMs.setValue(settings.joinDelayMs());
        versionBox.setSelectedItem(settings.protocolVersion());
        readTimeout.setValue(settings.readTimeout());
        writeTimeout.setValue(settings.writeTimeout());
        connectTimeout.setValue(settings.connectTimeout());
        trySrv.setSelected(settings.trySrv());
        waitEstablished.setSelected(settings.waitEstablished());
    }

    @Override
    public BotSettings collectSettings() {
        return new BotSettings(
                hostInput.getText(),
                Integer.parseInt(portInput.getText()),
                (int) amount.getValue(),
                (int) joinDelayMs.getValue(),
                (ProtocolVersion) versionBox.getSelectedItem(),
                (int) readTimeout.getValue(),
                (int) writeTimeout.getValue(),
                (int) connectTimeout.getValue(),
                trySrv.isSelected(),
                waitEstablished.isSelected()
        );
    }
}
