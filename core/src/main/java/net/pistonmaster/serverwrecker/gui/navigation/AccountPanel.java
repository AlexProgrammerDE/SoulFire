/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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
import net.pistonmaster.serverwrecker.common.ProxyType;
import net.pistonmaster.serverwrecker.common.ServiceServer;
import net.pistonmaster.serverwrecker.gui.LoadAccountsListener;
import net.pistonmaster.serverwrecker.gui.LoadProxiesListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class AccountPanel extends NavigationItem {
    public static final JComboBox<ProxyType> proxyTypeCombo = new JComboBox<>();
    public static final JSpinner accPerProxy = new JSpinner();

    public AccountPanel(ServerWrecker serverWrecker, JFrame parent) {
        JPanel accounts = new JPanel();
        accounts.setLayout(new GridBagLayout());

        JButton loadAccounts = new JButton("Load Accounts");

        JFileChooser accountChooser = new JFileChooser();
        accountChooser.addChoosableFileFilter(new FileNameExtensionFilter("", "txt"));
        loadAccounts.addActionListener(new LoadAccountsListener(serverWrecker, parent, accountChooser));

        JPanel serviceSettingsPanel = new JPanel();

        serviceSettingsPanel.setLayout(new GridLayout(0, 1));

        JComboBox<ServiceServer> serviceBox = new JComboBox<>();
        Arrays.stream(ServiceServer.values()).forEach(serviceBox::addItem);

        serviceBox.setSelectedItem(ServiceServer.MOJANG);

        AtomicReference<JPanel> serviceSettings = new AtomicReference<>(getServiceSettings(ServiceServer.MOJANG));

        serviceBox.addActionListener(action -> {
            ServerWrecker.getInstance().setServiceServer((ServiceServer) serviceBox.getSelectedItem());

            serviceSettingsPanel.remove(serviceSettings.get());
            serviceSettings.set(getServiceSettings((ServiceServer) serviceBox.getSelectedItem()));
            serviceSettingsPanel.add(serviceSettings.get());
            serviceSettingsPanel.revalidate();

            ServerWrecker.getLogger().info("Switched auth servers to {}", ((ServiceServer) Objects.requireNonNull(serviceBox.getSelectedItem())).getName());
        });

        serviceSettingsPanel.add(serviceBox);
        serviceSettingsPanel.add(serviceSettings.get());

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

    private JPanel getServiceSettings(ServiceServer service) {
        JPanel serviceSettingsPanel = new JPanel();

        service.getConfigKeys().forEach(key -> {
            JLabel label = new JLabel(key);
            JTextField field = new JTextField();
            field.setText("");
            field.setSize(20, 60);
            serviceSettingsPanel.add(label);
            serviceSettingsPanel.add(field);
        });

        return serviceSettingsPanel;
    }

    @Override
    public String getNavigationName() {
        return "Accounts";
    }

    @Override
    public String getRightPanelContainerConstant() {
        return RightPanelContainer.ACCOUNT_MENU;
    }
}
