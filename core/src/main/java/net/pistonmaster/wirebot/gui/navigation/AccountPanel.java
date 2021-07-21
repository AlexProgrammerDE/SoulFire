package net.pistonmaster.wirebot.gui.navigation;

import net.pistonmaster.wirebot.WireBot;
import net.pistonmaster.wirebot.gui.LoadAccountsListener;
import net.pistonmaster.wirebot.gui.LoadProxiesListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class AccountPanel extends NavigationItem {
    public AccountPanel(WireBot wireBot, JFrame parent) {
        JButton loadAccounts = new JButton("Load Accounts");

        JFileChooser accountChooser = new JFileChooser();
        accountChooser.addChoosableFileFilter(new FileNameExtensionFilter("", "txt"));
        loadAccounts.addActionListener(new LoadAccountsListener(wireBot, parent, accountChooser));

        add(loadAccounts);

        JButton loadProxies = new JButton("Load proxies");

        JFileChooser proxiesChooser = new JFileChooser();
        proxiesChooser.addChoosableFileFilter(new FileNameExtensionFilter("", "txt"));
        loadProxies.addActionListener(new LoadProxiesListener(wireBot, parent, proxiesChooser));

        add(loadProxies);
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
