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
import net.pistonmaster.serverwrecker.common.ProxyType;
import net.pistonmaster.serverwrecker.gui.LoadProxiesListener;
import net.pistonmaster.serverwrecker.gui.libs.JEnumComboBox;
import net.pistonmaster.serverwrecker.gui.libs.NativeJFileChooser;
import net.pistonmaster.serverwrecker.settings.ProxySettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Path;

public class ProxyPanel extends NavigationItem implements SettingsDuplex<ProxySettings> {
    private final JEnumComboBox<ProxyType> proxyTypeCombo = new JEnumComboBox<>(ProxyType.class, ProxyType.SOCKS5);
    private final JSpinner botsPerProxy = new JSpinner();

    @Inject
    public ProxyPanel(ServerWrecker serverWrecker, JFrame parent) {
        setLayout(new GridLayout(0, 2));

        JButton loadProxies = new JButton("Load proxies");
        JFileChooser proxiesChooser = new NativeJFileChooser(Path.of(System.getProperty("user.dir")));

        proxiesChooser.addChoosableFileFilter(new FileNameExtensionFilter("Proxy list file", "txt"));
        loadProxies.addActionListener(new LoadProxiesListener(serverWrecker, parent, proxiesChooser));

        add(loadProxies);
        add(proxyTypeCombo);

        add(new JLabel("Accounts per proxy: "));
        botsPerProxy.setValue(-1);
        add(botsPerProxy);
    }

    @Override
    public String getNavigationName() {
        return "Proxies";
    }

    @Override
    public String getNavigationId() {
        return "proxy-menu";
    }

    @Override
    public void onSettingsChange(ProxySettings settings) {
        proxyTypeCombo.setSelectedItem(settings.proxyType());
        botsPerProxy.setValue(settings.botsPerProxy());
    }

    @Override
    public ProxySettings collectSettings() {
        return new ProxySettings(
                proxyTypeCombo.getSelectedEnum(),
                (int) botsPerProxy.getValue()
        );
    }
}
