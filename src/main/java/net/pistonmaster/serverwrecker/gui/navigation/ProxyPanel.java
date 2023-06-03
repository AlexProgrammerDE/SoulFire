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
import net.pistonmaster.serverwrecker.common.SWProxy;
import net.pistonmaster.serverwrecker.gui.libs.JEnumComboBox;
import net.pistonmaster.serverwrecker.gui.libs.NativeJFileChooser;
import net.pistonmaster.serverwrecker.settings.ProxySettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ProxyPanel extends NavigationItem implements SettingsDuplex<ProxySettings> {
    private final JSpinner botsPerProxy = new JSpinner();

    @Inject
    public ProxyPanel(ServerWrecker serverWrecker, JFrame parent) {
        serverWrecker.getSettingsManager().registerDuplex(ProxySettings.class, this);

        setLayout(new GridLayout(0, 2));

        JButton loadProxies = new JButton("Load proxies");
        JFileChooser proxiesChooser = new NativeJFileChooser(Path.of(System.getProperty("user.dir")));

        proxiesChooser.addChoosableFileFilter(new FileNameExtensionFilter("Proxy list file", "txt"));
        loadProxies.addActionListener(new LoadProxiesListener(serverWrecker, parent, proxiesChooser));

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

    private record LoadProxiesListener(ServerWrecker serverWrecker, JFrame frame, JFileChooser fileChooser) implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            int returnVal = fileChooser.showOpenDialog(frame);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                return;
            }

            Path proxyFile = fileChooser.getSelectedFile().toPath();
            serverWrecker.getLogger().info("Opening: {}.", proxyFile.getFileName());

            serverWrecker.getThreadPool().submit(() -> {
                try {
                    List<SWProxy> proxies = new ArrayList<>();

                    try (Stream<String> lines = Files.lines(proxyFile)) {
                        lines.distinct().forEach(line -> {
                            String[] split = line.split(":");

                            String host = split[0];
                            int port = Integer.parseInt(split[1]);

                            // TODO: Reimplement proxy management
                            if (split.length > 3) {
                                proxies.add(new SWProxy(ProxyType.SOCKS5, new InetSocketAddress(host, port), split[2], split[3]));
                            } else {
                                proxies.add(new SWProxy(ProxyType.SOCKS5, new InetSocketAddress(host, port), null, null));
                            }
                        });
                    }

                    serverWrecker.getAvailableProxies().clear();
                    serverWrecker.getAvailableProxies().addAll(proxies);

                    serverWrecker.getLogger().info("Loaded {} proxies", proxies.size());
                } catch (Exception ex) {
                    serverWrecker.getLogger().error(null, ex);
                }
            });
        }
    }
}
