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

import javafx.stage.FileChooser;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.common.ProxyType;
import net.pistonmaster.serverwrecker.gui.libs.JEnumComboBox;
import net.pistonmaster.serverwrecker.gui.libs.JFXFileHelper;
import net.pistonmaster.serverwrecker.proxy.ProxySettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;

public class ProxyPanel extends NavigationItem implements SettingsDuplex<ProxySettings> {
    private final JSpinner botsPerProxy = new JSpinner();

    @Inject
    public ProxyPanel(ServerWrecker serverWrecker, JFrame parent) {
        serverWrecker.getSettingsManager().registerDuplex(ProxySettings.class, this);

        setLayout(new GridLayout(0, 2));

        JButton loadProxies = new JButton("Load proxies");

        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(Path.of(System.getProperty("user.dir")).toFile());
        chooser.setTitle("Load proxies");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Proxy list file", "txt"));

        loadProxies.addActionListener(new LoadProxiesListener(serverWrecker, parent, chooser));

        add(loadProxies);
        JEnumComboBox<ProxyType> proxyTypeCombo = new JEnumComboBox<>(ProxyType.class, ProxyType.SOCKS5);
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
        botsPerProxy.setValue(settings.botsPerProxy());
    }

    @Override
    public ProxySettings collectSettings() {
        return new ProxySettings(
                (int) botsPerProxy.getValue()
        );
    }

    private record LoadProxiesListener(ServerWrecker serverWrecker, JFrame frame,
                                       FileChooser chooser) implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            Path proxyFile = JFXFileHelper.showOpenDialog(chooser);
            if (proxyFile == null) {
                return;
            }

            serverWrecker.getLogger().info("Opening: {}.", proxyFile.getFileName());

            serverWrecker.getThreadPool().submit(() -> {
                try {
                    serverWrecker.getProxyRegistry().loadFromFile(proxyFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
