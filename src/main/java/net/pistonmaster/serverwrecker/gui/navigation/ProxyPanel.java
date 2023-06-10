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
import net.pistonmaster.serverwrecker.gui.libs.JEnumComboBox;
import net.pistonmaster.serverwrecker.gui.libs.JFXFileHelper;
import net.pistonmaster.serverwrecker.proxy.ProxyRegistry;
import net.pistonmaster.serverwrecker.proxy.ProxySettings;
import net.pistonmaster.serverwrecker.proxy.ProxyType;
import net.pistonmaster.serverwrecker.proxy.SWProxy;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProxyPanel extends NavigationItem implements SettingsDuplex<ProxySettings> {
    private final JSpinner botsPerProxy = new JSpinner();

    @Inject
    public ProxyPanel(ServerWrecker serverWrecker, JFrame parent) {
        serverWrecker.getSettingsManager().registerDuplex(ProxySettings.class, this);

        setLayout(new GridLayout(2, 1, 10, 10));

        JPanel proxyOptionsPanel = new JPanel();
        proxyOptionsPanel.setLayout(new GridLayout(2, 1, 10, 10));

        JPanel addProxyPanel = new JPanel();
        addProxyPanel.setLayout(new GridLayout(1, 3, 10, 10));

        addProxyPanel.add(createProxyLoadButton(serverWrecker, parent, ProxyType.HTTP));
        addProxyPanel.add(createProxyLoadButton(serverWrecker, parent, ProxyType.SOCKS4));
        addProxyPanel.add(createProxyLoadButton(serverWrecker, parent, ProxyType.SOCKS5));

        proxyOptionsPanel.add(addProxyPanel);

        JPanel proxySettingsPanel = new JPanel();
        proxySettingsPanel.setLayout(new GridLayout(0, 2));

        proxySettingsPanel.add(new JLabel("Accounts per proxy: "));
        botsPerProxy.setValue(ProxySettings.DEFAULT_BOTS_PER_PROXY);
        proxySettingsPanel.add(botsPerProxy);

        proxyOptionsPanel.add(proxySettingsPanel);

        add(proxyOptionsPanel);

        JPanel proxyListPanel = new JPanel();
        proxyListPanel.setLayout(new GridLayout(1, 1));

        String[] columnNames = {"IP", "Port", "Username", "Password", "Type", "Enabled"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            final Class<?>[] columnTypes = new Class<?>[]{
                    Object.class, Integer.class, Object.class, Object.class, ProxyType.class, Boolean.class
            };

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnTypes[columnIndex];
            }
        };

        JTable proxyList = new JTable(model);

        serverWrecker.getProxyRegistry().addLoadHook(() -> {
            model.getDataVector().removeAllElements();

            ProxyRegistry registry = serverWrecker.getProxyRegistry();
            int registrySize = registry.getProxies().size();
            Object[][] dataVector = new Object[registrySize][];
            for (int i = 0; i < registrySize; i++) {
                SWProxy proxy = registry.getProxies().get(i);

                dataVector[i] = new Object[]{
                        proxy.host(),
                        proxy.port(),
                        proxy.username(),
                        proxy.password(),
                        proxy.type(),
                        proxy.enabled()
                };
            }

            model.setDataVector(dataVector, columnNames);

            proxyList.getColumnModel().getColumn(4)
                    .setCellEditor(new DefaultCellEditor(new JEnumComboBox<>(ProxyType.class)));

            model.fireTableDataChanged();
        });

        proxyList.addPropertyChangeListener(evt -> {
            if ("tableCellEditor".equals(evt.getPropertyName()) && !proxyList.isEditing()) {
                List<SWProxy> proxies = new ArrayList<>();

                for (int i = 0; i < proxyList.getRowCount(); i++) {
                    Object[] row = new Object[proxyList.getColumnCount()];
                    for (int j = 0; j < proxyList.getColumnCount(); j++) {
                        row[j] = proxyList.getValueAt(i, j);
                    }

                    String host = (String) row[0];
                    int port = (int) row[1];
                    String username = (String) row[2];
                    String password = (String) row[3];
                    ProxyType type = (ProxyType) row[4];
                    boolean enabled = (boolean) row[5];

                    proxies.add(new SWProxy(type, host, port, username, password, enabled));
                }

                serverWrecker.getProxyRegistry().setProxies(proxies);
            }
        });

        JScrollPane scrollPane = new JScrollPane(proxyList);

        proxyListPanel.add(scrollPane);

        add(proxyListPanel);
    }

    private JButton createProxyLoadButton(ServerWrecker serverWrecker, JFrame parent, ProxyType type) {
        String loadText = String.format("Load %s proxies", type.name());
        String typeText = String.format("%s list file", type.name());
        JButton button = new JButton(loadText);

        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(Path.of(System.getProperty("user.dir")).toFile());
        chooser.setTitle(loadText);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(typeText, "*.txt"));

        button.addActionListener(new LoadProxiesListener(serverWrecker, parent, chooser, type));
        return button;
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
                                       FileChooser chooser, ProxyType proxyType) implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            Path proxyFile = JFXFileHelper.showOpenDialog(chooser);
            if (proxyFile == null) {
                return;
            }

            serverWrecker.getLogger().info("Opening: {}.", proxyFile.getFileName());

            serverWrecker.getThreadPool().submit(() -> {
                try {
                    serverWrecker.getProxyRegistry().loadFromFile(proxyFile, proxyType);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
