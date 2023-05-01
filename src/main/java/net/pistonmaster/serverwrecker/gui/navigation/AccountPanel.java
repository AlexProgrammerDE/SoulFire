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
import net.pistonmaster.serverwrecker.common.AuthService;
import net.pistonmaster.serverwrecker.common.ProxyType;
import net.pistonmaster.serverwrecker.gui.LoadAccountsListener;
import net.pistonmaster.serverwrecker.gui.LoadProxiesListener;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class AccountPanel extends NavigationItem {
    // TODO: Redo config flow, this is bad
    public static final JComboBox<ProxyType> proxyTypeCombo = new JComboBox<>();
    public static final JSpinner accPerProxy = new JSpinner();
    public static final JComboBox<AuthService> serviceBox = new JComboBox<>();
    private final ServerWrecker serverWrecker;

    @Inject
    public AccountPanel(ServerWrecker serverWrecker, JFrame parent) {
        this.serverWrecker = serverWrecker;

        JPanel accounts = new JPanel();
        accounts.setLayout(new GridLayout(0, 2));

        JButton loadAccounts = new JButton("Load Accounts");

        JFileChooser accountChooser = new JFileChooser();
        accountChooser.addChoosableFileFilter(new FileNameExtensionFilter("", "txt"));
        loadAccounts.addActionListener(new LoadAccountsListener(serverWrecker, parent, accountChooser));

        JPanel serviceSettingsPanel = new JPanel();

        serviceSettingsPanel.setLayout(new GridLayout(0, 1));

        Arrays.stream(AuthService.values()).forEach(serviceBox::addItem);

        serviceBox.setSelectedItem(AuthService.OFFLINE);

        AtomicReference<JPanel> currentServiceSettings = new AtomicReference<>(getServiceSettings(AuthService.OFFLINE));
        serviceBox.addActionListener(action -> {
            refreshSettings(serviceSettingsPanel, currentServiceSettings.get(), (AuthService) serviceBox.getSelectedItem());

            serverWrecker.getLogger().info("Switched auth servers to {}", ((AuthService) Objects.requireNonNull(serviceBox.getSelectedItem())).getDisplayName());
        });

        serviceSettingsPanel.add(serviceBox);
        serviceSettingsPanel.add(currentServiceSettings.get());

        accounts.add(loadAccounts);
        accounts.add(serviceSettingsPanel);

        add(accounts);

        JPanel proxies = new JPanel();
        JButton loadProxies = new JButton("Load proxies");
        JFileChooser proxiesChooser = new JFileChooser();

        proxiesChooser.addChoosableFileFilter(new FileNameExtensionFilter("", "txt"));
        loadProxies.addActionListener(new LoadProxiesListener(serverWrecker, parent, proxiesChooser));

        Arrays.stream(ProxyType.values()).forEach(proxyTypeCombo::addItem);

        proxyTypeCombo.setSelectedItem(ProxyType.SOCKS5);

        proxies.add(loadProxies);
        proxies.add(proxyTypeCombo);

        proxies.add(new JLabel("Accounts per proxy: "));
        accPerProxy.setValue(-1);
        proxies.add(accPerProxy);

        add(proxies);
    }

    private JPanel getServiceSettings(AuthService service) {
        JPanel serviceSettingsPanel = new JPanel();

        service.getConfigKeys().forEach(key -> {
            JLabel label = new JLabel(key);
            JTextField field = new JTextField();
            field.setText(serverWrecker.getServiceServerConfig().getOrDefault(key, ""));
            field.setSize(40, 60);
            serviceSettingsPanel.add(label);
            serviceSettingsPanel.add(field);

            field.addActionListener(action ->
                    serverWrecker.getServiceServerConfig().put(key, field.getText()));

            field.addActionListener(action ->
                    System.out.println(field.getText()));
        });

        return serviceSettingsPanel;
    }

    private void refreshSettings(JPanel serviceSettingsPanel, JPanel currentPanel, AuthService authService) {
        if (currentPanel != null) {
            serviceSettingsPanel.remove(currentPanel);
        }

        serviceSettingsPanel.add(getServiceSettings(authService));
        serviceSettingsPanel.revalidate();
    }

    @Override
    public String getNavigationName() {
        return "Accounts";
    }

    @Override
    public String getNavigationId() {
        return RightPanelContainer.ACCOUNT_MENU;
    }
}
