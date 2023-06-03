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
import net.pistonmaster.serverwrecker.auth.AuthType;
import net.pistonmaster.serverwrecker.gui.libs.JEnumComboBox;
import net.pistonmaster.serverwrecker.gui.libs.NativeJFileChooser;
import net.pistonmaster.serverwrecker.settings.AccountSettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class AccountPanel extends NavigationItem implements SettingsDuplex<AccountSettings> {
    private final JEnumComboBox<AuthType> serviceBox = new JEnumComboBox<>(AuthType.class, AuthType.OFFLINE);

    @Inject
    public AccountPanel(ServerWrecker serverWrecker, JFrame parent) {
        serverWrecker.getSettingsManager().registerDuplex(AccountSettings.class, this);

        setLayout(new GridLayout(0, 2));

        JButton loadAccounts = new JButton("Load Accounts");

        JFileChooser accountChooser = new NativeJFileChooser(Path.of(System.getProperty("user.dir")));
        accountChooser.addChoosableFileFilter(new FileNameExtensionFilter("Account list file", "txt", "json"));
        loadAccounts.addActionListener(new LoadAccountsListener(serverWrecker, parent, accountChooser));

        JPanel serviceSettingsPanel = new JPanel();

        serviceSettingsPanel.setLayout(new GridLayout(0, 1));

        serviceBox.addActionListener(action -> {
            serverWrecker.getLogger().info("Switched auth servers to {}", Objects.requireNonNull(serviceBox.getSelectedEnum()).getDisplayName());
        });

        serviceSettingsPanel.add(serviceBox);

        add(loadAccounts);
        add(serviceSettingsPanel);
    }

    @Override
    public String getNavigationName() {
        return "Accounts";
    }

    @Override
    public String getNavigationId() {
        return "account-menu";
    }

    @Override
    public void onSettingsChange(AccountSettings settings) {
    }

    @Override
    public AccountSettings collectSettings() {
        return new AccountSettings();
    }

    private record LoadAccountsListener(ServerWrecker serverWrecker, JFrame frame, JFileChooser fileChooser) implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            int returnVal = fileChooser.showOpenDialog(frame);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                return;
            }

            Path accountFile = fileChooser.getSelectedFile().toPath();
            serverWrecker.getLogger().info("Opening: {}", accountFile.getFileName());

            serverWrecker.getThreadPool().submit(() -> {
                try {
                    serverWrecker.getAccountRegistry().loadFromFile(Files.readString(accountFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

}
