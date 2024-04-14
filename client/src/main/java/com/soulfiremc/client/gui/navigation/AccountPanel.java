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
package com.soulfiremc.client.gui.navigation;

import com.soulfiremc.client.gui.GUIFrame;
import com.soulfiremc.client.gui.GUIManager;
import com.soulfiremc.client.gui.popups.ImportTextDialog;
import com.soulfiremc.settings.account.AuthType;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.util.BuiltinSettingsConstants;
import com.soulfiremc.util.EnabledWrapper;
import com.soulfiremc.util.SFPathConstants;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.UUID;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import net.lenni0451.commons.swing.GBC;

public class AccountPanel extends NavigationItem {
  @Inject
  public AccountPanel(GUIManager guiManager, GUIFrame parent, CardsContainer cardsContainer) {
    setLayout(new GridBagLayout());

    var accountSettingsPanel = new JPanel();
    accountSettingsPanel.setLayout(new GridBagLayout());

    GeneratedPanel.addComponents(
      accountSettingsPanel,
      cardsContainer.getByNamespace(BuiltinSettingsConstants.ACCOUNT_SETTINGS_ID),
      guiManager.clientSettingsManager());

    GBC.create(this).grid(0, 0).fill(GBC.HORIZONTAL).weightx(1).add(accountSettingsPanel);

    var toolBar = new JToolBar();
    GBC.create(this).grid(0, 1).insets(10, 4, -5, 4).fill(GBC.HORIZONTAL).weightx(0).add(toolBar);

    var columnNames = new String[] {"Username", "UUID", "Type", "Enabled"};
    var enabledColumn = 3; // Index of the enabled column
    var columnTypes = new Class<?>[] {String.class, UUID.class, AuthType.class, Boolean.class};
    var model =
      new DefaultTableModel(columnNames, 0) {
        @Override
        public Class<?> getColumnClass(int columnIndex) {
          return columnTypes[columnIndex];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
          return column == enabledColumn;
        }
      };

    var accountList = new JTable(model);

    var accountRegistry = guiManager.clientSettingsManager().accountRegistry();
    accountRegistry.addLoadHook(
      () -> {
        model.getDataVector().removeAllElements();

        var accounts = accountRegistry.accounts();
        var dataVector = new Object[accounts.size()][];
        var i = 0;
        for (var account : accounts) {
          dataVector[i++] =
            new Object[] {
              account.value().lastKnownName(),
              account.value().profileId(),
              account.value().authType(),
              account.enabled()
            };
        }

        model.setDataVector(dataVector, columnNames);
        model.fireTableDataChanged();
      });

    Runnable reconstructFromTable =
      () -> {
        var accounts = new ArrayList<EnabledWrapper<MinecraftAccount>>();

        var rowCount = accountList.getRowCount();
        var columnCount = accountList.getColumnCount();
        for (var row = 0; row < rowCount; row++) {
          var rowData = new Object[columnCount];
          for (var column = 0; column < columnCount; column++) {
            rowData[column] = accountList.getValueAt(row, column);
          }

          var username = (String) rowData[0];
          var profileId = (UUID) rowData[1];
          var authType = (AuthType) rowData[2];
          var enabled = (boolean) rowData[3];

          var account = accountRegistry.getAccount(profileId).orElseThrow();

          accounts.add(
            new EnabledWrapper<>(
              enabled,
              new MinecraftAccount(authType, profileId, username, account.accountData())));
        }

        accountRegistry.setAccounts(accounts);
      };
    accountList.addPropertyChangeListener(
      evt -> {
        if ("tableCellEditor".equals(evt.getPropertyName()) && !accountList.isEditing()) {
          reconstructFromTable.run();
        }
      });

    var scrollPane = new JScrollPane(accountList);

    GBC.create(this).grid(0, 2).fill(GBC.BOTH).weight(1, 1).add(scrollPane);

    toolBar.setFloatable(false);
    var addButton = new JButton("+");
    addButton.setToolTipText("Add accounts to the list");
    addButton.addMouseListener(
      new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          var menu = new JPopupMenu();
          menu.add(createAccountLoadButton(guiManager, parent, AuthType.OFFLINE));
          menu.add(createAccountLoadButton(guiManager, parent, AuthType.MICROSOFT_JAVA));
          menu.add(createAccountLoadButton(guiManager, parent, AuthType.MICROSOFT_BEDROCK));
          menu.add(createAccountLoadButton(guiManager, parent, AuthType.THE_ALTENING));
          menu.add(createAccountLoadButton(guiManager, parent, AuthType.EASY_MC));
          menu.show(e.getComponent(), e.getX(), e.getY());
        }
      });
    var removeButton = new JButton("-");
    removeButton.setToolTipText("Remove selected accounts from the list");
    removeButton.addActionListener(
      e -> {
        var selectedRows = accountList.getSelectedRows();
        for (var i = selectedRows.length - 1; i >= 0; i--) {
          model.removeRow(selectedRows[i]);
        }
        reconstructFromTable.run();
      });

    toolBar.add(addButton);
    toolBar.add(removeButton);
    toolBar.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
    toolBar.setBackground(UIManager.getColor("Table.background"));
  }

  private static JMenuItem createAccountLoadButton(
    GUIManager guiManager, GUIFrame parent, AuthType type) {
    var button = new JMenuItem(type.toString());

    button.addActionListener(
      e ->
        new ImportTextDialog(
          SFPathConstants.WORKING_DIRECTORY,
          "Add %s accounts".formatted(type),
          "%s list file".formatted(String.valueOf(type)),
          guiManager,
          parent,
          text ->
            guiManager
              .clientSettingsManager()
              .accountRegistry()
              .loadFromString(text, type, null)));

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
