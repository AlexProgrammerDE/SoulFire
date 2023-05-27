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
import net.pistonmaster.serverwrecker.common.SWOptions;
import net.pistonmaster.serverwrecker.settings.SettingsDuplex;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettingsPanel extends NavigationItem implements SettingsDuplex<SWOptions> {
    private final JTextField hostInput;
    private final JTextField portInput;
    private final JSpinner joinDelayMs;
    private final JCheckBox disableWaitEstablished;
    private final JCheckBox autoRegister;
    private final JTextField registerCommand;
    private final JTextField loginCommand;
    private final JTextField captchaCommand;
    private final JTextField passwordFormat;
    private final JSpinner amount;
    private final JTextField nameFormat;
    private final JComboBox<ProtocolVersion> versionBox;
    private final JSpinner readTimeout;
    private final JSpinner writeTimeout;
    private final JSpinner connectTimeout;
    private final JCheckBox autoReconnect;
    private final JCheckBox autoRespawn;

    @Inject
    public SettingsPanel(ServerWrecker serverWrecker) {
        super();
        serverWrecker.getSettingsManager().registerDuplex(SWOptions.class, this);

        setLayout(new GridLayout(0, 2));

        add(new JLabel("Host: "));
        hostInput = new JTextField("127.0.0.1");
        add(hostInput);

        add(new JLabel("Port: "));
        portInput = new JTextField("25565");
        add(portInput);

        add(new JLabel("Join delay (ms): "));
        joinDelayMs = new JSpinner();
        joinDelayMs.setValue(1000);
        add(joinDelayMs);

        add(new JLabel("Disable wait established: "));
        disableWaitEstablished = new JCheckBox();
        add(disableWaitEstablished);

        add(new JLabel("Auto Register: "));
        autoRegister = new JCheckBox();
        add(autoRegister);

        add(new JLabel("Register Command: "));
        registerCommand = new JTextField("/register %password% %password%");
        add(registerCommand);

        add(new JLabel("Login Command: "));
        loginCommand = new JTextField("/login %password%");
        add(loginCommand);

        add(new JLabel("Captcha Command: "));
        captchaCommand = new JTextField("/captcha %captcha%");
        add(captchaCommand);

        add(new JLabel("Password Format: "));
        passwordFormat = new JTextField("ServerWrecker");
        add(passwordFormat);

        add(new JLabel("Amount: "));
        amount = new JSpinner();
        amount.setValue(20);
        add(amount);

        add(new JLabel("Name Format: "));
        nameFormat = new JTextField("Bot%d");
        add(nameFormat);

        add(new JLabel("Version: "));
        versionBox = new JComboBox<>();
        registerVersions();
        add(versionBox);

        add(new JLabel("Read Timeout: "));
        readTimeout = new JSpinner();
        readTimeout.setValue(30);
        add(readTimeout);

        add(new JLabel("Write Timeout: "));
        writeTimeout = new JSpinner();
        writeTimeout.setValue(0);
        add(writeTimeout);

        add(new JLabel("Connect Timeout: "));
        connectTimeout = new JSpinner();
        connectTimeout.setValue(30);
        add(connectTimeout);

        add(new JLabel("Auto Reconnect: "));
        autoReconnect = new JCheckBox();
        autoReconnect.setSelected(true);
        add(autoReconnect);

        add(new JLabel("Auto Respawn: "));
        autoRespawn = new JCheckBox();
        autoRespawn.setSelected(true);
        add(autoRespawn);
    }

    public void registerVersions() {
        versionBox.removeAllItems();
        List<ProtocolVersion> versions = new ArrayList<>(SWConstants.getVersionsSorted());
        Collections.reverse(versions);
        versions.forEach(versionBox::addItem);
    }

    @Override
    public String getNavigationName() {
        return "Settings";
    }

    @Override
    public String getNavigationId() {
        return RightPanelContainer.SETTINGS_MENU;
    }

    @Override
    public void onSettingsChange(SWOptions settings) {
        hostInput.setText(settings.host());
        portInput.setText(String.valueOf(settings.port()));
        amount.setValue(settings.amount());
        joinDelayMs.setValue(settings.joinDelayMs());
        disableWaitEstablished.setSelected(!settings.waitEstablished());
        nameFormat.setText(settings.botNameFormat());
        versionBox.setSelectedItem(settings.protocolVersion());
        autoRegister.setSelected(settings.autoRegister());
        DeveloperPanel.debug.setSelected(settings.debug());
        AccountPanel.proxyTypeCombo.setSelectedEnum(settings.proxyType());
        AccountPanel.botsPerProxy.setValue(settings.botsPerProxy());
        readTimeout.setValue(settings.readTimeout());
        writeTimeout.setValue(settings.writeTimeout());
        connectTimeout.setValue(settings.connectTimeout());
        registerCommand.setText(settings.registerCommand());
        loginCommand.setText(settings.loginCommand());
        captchaCommand.setText(settings.captchaCommand());
        passwordFormat.setText(settings.passwordFormat());
        autoReconnect.setSelected(settings.autoReconnect());
        autoRespawn.setSelected(settings.autoRespawn());
        AccountPanel.serviceBox.setSelectedEnum(settings.authType());
    }

    @Override
    public SWOptions collectSettings() {
        return new SWOptions(
                hostInput.getText(),
                Integer.parseInt(portInput.getText()),
                (int) amount.getValue(),
                (int) joinDelayMs.getValue(),
                !disableWaitEstablished.isSelected(),
                nameFormat.getText(),
                (ProtocolVersion) versionBox.getSelectedItem(),
                autoRegister.isSelected(),
                DeveloperPanel.debug.isSelected(),
                AccountPanel.proxyTypeCombo.getSelectedEnum(),
                (Integer) AccountPanel.botsPerProxy.getValue(),
                (int) readTimeout.getValue(),
                (int) writeTimeout.getValue(),
                (int) connectTimeout.getValue(),
                registerCommand.getText(),
                loginCommand.getText(),
                captchaCommand.getText(),
                passwordFormat.getText(),
                autoReconnect.isSelected(),
                autoRespawn.isSelected(),
                AccountPanel.serviceBox.getSelectedEnum());
    }
}
