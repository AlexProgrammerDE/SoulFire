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
import net.pistonmaster.serverwrecker.common.ProxyType;
import net.pistonmaster.serverwrecker.gui.LoadAccountsListener;
import net.pistonmaster.serverwrecker.gui.LoadProxiesListener;
import net.pistonmaster.serverwrecker.gui.libs.JEnumComboBox;
import net.pistonmaster.serverwrecker.gui.libs.NativeJFileChooser;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Path;
import java.util.Objects;

public class AccountPanel extends NavigationItem {
    // TODO: Redo config flow, this is bad
    public static final JEnumComboBox<ProxyType> proxyTypeCombo = new JEnumComboBox<>(ProxyType.class, ProxyType.SOCKS5);
    public static final JSpinner botsPerProxy = new JSpinner();
    public static final JEnumComboBox<AuthType> serviceBox = new JEnumComboBox<>(AuthType.class, AuthType.OFFLINE);
    private final ServerWrecker serverWrecker;

    @Inject
    public AccountPanel(ServerWrecker serverWrecker, JFrame parent) {
        this.serverWrecker = serverWrecker;
        JPanel accounts = new JPanel();
        accounts.setLayout(new GridLayout(0, 2));

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

        accounts.add(loadAccounts);
        accounts.add(serviceSettingsPanel);

        add(accounts);

        JPanel proxies = new JPanel();
        JButton loadProxies = new JButton("Load proxies");
        JFileChooser proxiesChooser = new NativeJFileChooser(Path.of(System.getProperty("user.dir")));

        proxiesChooser.addChoosableFileFilter(new FileNameExtensionFilter("Proxy list file", "txt"));
        loadProxies.addActionListener(new LoadProxiesListener(serverWrecker, parent, proxiesChooser));

        proxies.add(loadProxies);
        proxies.add(proxyTypeCombo);

        proxies.add(new JLabel("Accounts per proxy: "));
        botsPerProxy.setValue(-1);
        proxies.add(botsPerProxy);

        add(proxies);
    }

    @Override
    public String getNavigationName() {
        return "Accounts";
    }

    @Override
    public String getNavigationId() {
        return ButtonPanelContainer.ACCOUNT_MENU;
    }
}
