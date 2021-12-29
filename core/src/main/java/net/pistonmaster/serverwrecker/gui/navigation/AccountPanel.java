/*
 * ServerWrecker
 *
 * Copyright (C) 2021 ServerWrecker
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
import java.util.Arrays;
import java.util.Objects;

public class AccountPanel extends NavigationItem {
    public static final JComboBox<ProxyType> proxyTypeCombo = new JComboBox<>();
    public static final JSpinner accPerProxy = new JSpinner();

    public AccountPanel(ServerWrecker wireBot, JFrame parent) {
        JPanel accounts = new JPanel();

        JButton loadAccounts = new JButton("Load Accounts");

        JFileChooser accountChooser = new JFileChooser();
        accountChooser.addChoosableFileFilter(new FileNameExtensionFilter("", "txt"));
        loadAccounts.addActionListener(new LoadAccountsListener(wireBot, parent, accountChooser));

        JComboBox<ServiceServer> serviceBox = new JComboBox<>();
        Arrays.stream(ServiceServer.values()).forEach(serviceBox::addItem);

        serviceBox.setSelectedItem(ServiceServer.MOJANG);

        serviceBox.addActionListener(action -> {
            ServerWrecker.getInstance().setServiceServer((ServiceServer) serviceBox.getSelectedItem());
            ServerWrecker.getLogger().info("Switched auth servers to " + ((ServiceServer) Objects.requireNonNull(serviceBox.getSelectedItem())).getName());
        });

        accounts.add(loadAccounts);
        accounts.add(serviceBox);

        add(accounts);

        JPanel proxies = new JPanel();
        JButton loadProxies = new JButton("Load proxies");
        JFileChooser proxiesChooser = new JFileChooser();

        proxiesChooser.addChoosableFileFilter(new FileNameExtensionFilter("", "txt"));
        loadProxies.addActionListener(new LoadProxiesListener(wireBot, parent, proxiesChooser));

        Arrays.stream(ProxyType.values()).forEach(proxyTypeCombo::addItem);

        proxyTypeCombo.setSelectedItem(ProxyType.SOCKS5);

        proxies.add(loadProxies);
        proxies.add(proxyTypeCombo);

        proxies.add(new JLabel("Accounts per proxy: "));
        accPerProxy.setValue(-1);
        proxies.add(accPerProxy);

        add(proxies);
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
