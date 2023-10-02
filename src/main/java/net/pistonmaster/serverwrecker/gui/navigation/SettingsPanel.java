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
import net.pistonmaster.serverwrecker.gui.libs.JMinMaxHelper;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.settings.BotSettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class SettingsPanel extends NavigationItem implements SettingsDuplex<BotSettings> {
    private final JTextField hostInput = new JTextField(BotSettings.DEFAULT_HOST);
    private final JTextField portInput = new JTextField(String.valueOf(BotSettings.DEFAULT_PORT));
    private final JCheckBox trySrv = new PresetJCheckBox(BotSettings.DEFAULT_TRY_SRV);
    private final JSpinner minJoinDelayMs = new JSpinner();
    private final JSpinner maxJoinDelayMs = new JSpinner();
    private final JSpinner amount = new JSpinner();
    private final JComboBox<ProtocolVersion> versionBox = new JComboBox<>();
    private final JSpinner readTimeout = new JSpinner();
    private final JSpinner writeTimeout = new JSpinner();
    private final JSpinner connectTimeout = new JSpinner();
    private final JSpinner concurrentConnects = new JSpinner();

    @Inject
    public SettingsPanel(ServerWrecker serverWrecker) {
        serverWrecker.getSettingsManager().registerDuplex(BotSettings.class, this);

        setLayout(new GridLayout(0, 2));

        add(new JLabel("Host: "));
        add(hostInput);

        add(new JLabel("Port: "));
        add(portInput);

        add(new JLabel("Min join delay (ms): "));
        minJoinDelayMs.setValue(BotSettings.DEFAULT_MIN_JOIN_DELAY_MS);
        add(minJoinDelayMs);

        add(new JLabel("Max join delay (ms): "));
        maxJoinDelayMs.setValue(BotSettings.DEFAULT_MAX_JOIN_DELAY_MS);
        add(maxJoinDelayMs);

        JMinMaxHelper.applyLink(minJoinDelayMs, maxJoinDelayMs);

        add(new JLabel("Amount: "));
        amount.setValue(BotSettings.DEFAULT_AMOUNT);
        add(amount);

        add(new JLabel("Version: "));
        versionBox.setRenderer(new ProtocolVersionRenderer());
        registerVersions();
        add(versionBox);

        add(new JLabel("Read Timeout: "));
        readTimeout.setValue(BotSettings.DEFAULT_READ_TIMEOUT);
        add(readTimeout);

        add(new JLabel("Write Timeout: "));
        writeTimeout.setValue(BotSettings.DEFAULT_WRITE_TIMEOUT);
        add(writeTimeout);

        add(new JLabel("Connect Timeout: "));
        connectTimeout.setValue(BotSettings.DEFAULT_CONNECT_TIMEOUT);
        add(connectTimeout);

        add(new JLabel("Try SRV record resolving: "));
        add(trySrv);

        add(new JLabel("Concurrent Connects: "));
        concurrentConnects.setValue(BotSettings.DEFAULT_CONCURRENT_CONNECTS);
        add(concurrentConnects);
    }

    public void registerVersions() {
        versionBox.removeAllItems();
        var versions = new ArrayList<>(SWConstants.getVersionsSorted());
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
        minJoinDelayMs.setValue(settings.minJoinDelayMs());
        maxJoinDelayMs.setValue(settings.maxJoinDelayMs());
        versionBox.setSelectedItem(settings.protocolVersion());
        readTimeout.setValue(settings.readTimeout());
        writeTimeout.setValue(settings.writeTimeout());
        connectTimeout.setValue(settings.connectTimeout());
        trySrv.setSelected(settings.trySrv());
        concurrentConnects.setValue(settings.concurrentConnects());
    }

    @Override
    public BotSettings collectSettings() {
        return new BotSettings(
                hostInput.getText(),
                Integer.parseInt(portInput.getText()),
                (int) amount.getValue(),
                (int) minJoinDelayMs.getValue(),
                (int) maxJoinDelayMs.getValue(),
                (ProtocolVersion) versionBox.getSelectedItem(),
                (int) readTimeout.getValue(),
                (int) writeTimeout.getValue(),
                (int) connectTimeout.getValue(),
                trySrv.isSelected(),
                (int) concurrentConnects.getValue()
        );
    }

    private static class ProtocolVersionRenderer extends BasicComboBoxRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ProtocolVersion version) {
                if (SWConstants.isBedrock(version)) {
                    setText(String.format("%s (%s)", version.getName(), version.getVersion() - 1_000_000));
                } else if (SWConstants.isLegacy(version)) {
                    setText(String.format("%s (%s)", version.getName(), Math.abs(version.getVersion()) >> 2));
                } else {
                    setText(version.toString());
                }
            }

            return this;
        }
    }
}
