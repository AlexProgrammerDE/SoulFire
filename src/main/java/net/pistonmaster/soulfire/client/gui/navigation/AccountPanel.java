/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.soulfire.client.gui.navigation;

import net.pistonmaster.soulfire.account.AuthType;
import net.pistonmaster.soulfire.account.MinecraftAccount;
import net.pistonmaster.soulfire.client.gui.GUIFrame;
import net.pistonmaster.soulfire.client.gui.GUIManager;
import net.pistonmaster.soulfire.client.gui.libs.JEnumComboBox;
import net.pistonmaster.soulfire.client.gui.libs.SwingTextUtils;
import net.pistonmaster.soulfire.client.gui.popups.ImportTextDialog;
import net.pistonmaster.soulfire.util.BuiltinSettingsConstants;
import net.pistonmaster.soulfire.util.SWPathConstants;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class AccountPanel extends NavigationItem {
    @Inject
    public AccountPanel(GUIManager guiManager, GUIFrame parent, CardsContainer cardsContainer) {
        setLayout(new GridLayout(0, 1, 10, 10));

        var accountOptionsPanel = new JPanel();
        accountOptionsPanel.setLayout(new GridLayout(0, 1, 10, 10));

        var accountSettingsPanel = new JPanel();
        accountSettingsPanel.setLayout(new GridLayout(0, 2));

        GeneratedPanel.addComponents(accountSettingsPanel, cardsContainer.getByNamespace(BuiltinSettingsConstants.ACCOUNT_SETTINGS_ID), guiManager.settingsManager());

        accountOptionsPanel.add(accountSettingsPanel);

        var addAccountPanel = new JPanel();
        addAccountPanel.setLayout(new GridLayout(0, 3, 10, 10));

        addAccountPanel.add(createAccountLoadButton(guiManager, parent, AuthType.OFFLINE));
        addAccountPanel.add(createAccountLoadButton(guiManager, parent, AuthType.MICROSOFT_JAVA));
        addAccountPanel.add(createAccountLoadButton(guiManager, parent, AuthType.MICROSOFT_BEDROCK));
        addAccountPanel.add(createAccountLoadButton(guiManager, parent, AuthType.THE_ALTENING));
        addAccountPanel.add(createAccountLoadButton(guiManager, parent, AuthType.EASYMC));

        accountOptionsPanel.add(addAccountPanel);

        add(accountOptionsPanel);

        var accountListPanel = new JPanel();
        accountListPanel.setLayout(new GridLayout(0, 1));

        var columnNames = new String[]{"Username", "Type", "Enabled"};
        var model = new DefaultTableModel(columnNames, 0) {
            final Class<?>[] columnTypes = new Class<?>[]{String.class, AuthType.class, Boolean.class};

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnTypes[columnIndex];
            }
        };

        var accountList = new JTable(model);

        var accountRegistry = guiManager.settingsManager().accountRegistry();
        accountRegistry.addLoadHook(() -> {
            model.getDataVector().removeAllElements();

            var accounts = accountRegistry.getAccounts();
            var registrySize = accounts.size();
            var dataVector = new Object[registrySize][];
            for (var i = 0; i < registrySize; i++) {
                var account = accounts.get(i);

                dataVector[i] = new Object[]{
                        account.username(),
                        account.authType(),
                        account.enabled()
                };
            }

            model.setDataVector(dataVector, columnNames);

            accountList.getColumnModel().getColumn(1)
                    .setCellEditor(new DefaultCellEditor(new JEnumComboBox<>(AuthType.class)));

            model.fireTableDataChanged();
        });

        accountList.addPropertyChangeListener(evt -> {
            if ("tableCellEditor".equals(evt.getPropertyName()) && !accountList.isEditing()) {
                var accounts = new ArrayList<MinecraftAccount>();

                for (var i = 0; i < accountList.getRowCount(); i++) {
                    var row = new Object[accountList.getColumnCount()];
                    for (var j = 0; j < accountList.getColumnCount(); j++) {
                        row[j] = accountList.getValueAt(i, j);
                    }

                    var username = (String) row[0];
                    var authType = (AuthType) row[1];
                    var enabled = (boolean) row[2];

                    var account = accountRegistry.getAccount(username, authType);

                    accounts.add(new MinecraftAccount(authType, username, account.accountData(), enabled));
                }

                accountRegistry.setAccounts(accounts);
            }
        });

        var scrollPane = new JScrollPane(accountList);

        accountListPanel.add(scrollPane);

        add(accountListPanel);
    }

    private static JButton createAccountLoadButton(GUIManager guiManager, GUIFrame parent, AuthType type) {
        var button = new JButton(SwingTextUtils.htmlCenterText(String.format("Add %s accounts", type)));

        button.addActionListener(e -> new ImportTextDialog(
                SWPathConstants.WORKING_DIRECTORY,
                String.format("Add %s accounts", type),
                String.format("%s list file", type),
                guiManager,
                parent,
                text -> guiManager.settingsManager().accountRegistry().loadFromString(text, type)
        ));

        return button;
    }

    @Override
    public String getNavigationName() {
        return "Accounts";
    }

    @Override
    public String getNavigationId() {
        return "account-menu";
    }
}
