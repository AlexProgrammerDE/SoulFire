package net.pistonmaster.wirebot.gui.navigation;

import net.pistonmaster.wirebot.ServiceServer;
import net.pistonmaster.wirebot.WireBot;
import net.pistonmaster.wirebot.gui.LoadAccountsListener;
import net.pistonmaster.wirebot.gui.LoadProxiesListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.Arrays;
import java.util.Objects;

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

        JComboBox<ServiceServer> serviceBox = new JComboBox<>();
        Arrays.stream(ServiceServer.values())/*.map(ServiceServer::getName)*/.forEach(serviceBox::addItem);

        serviceBox.setSelectedItem(ServiceServer.MOJANG);

        serviceBox.addActionListener(action -> {
            WireBot.getInstance().setServiceServer((ServiceServer) serviceBox.getSelectedItem());
            WireBot.getLogger().info("Switched auth servers to " + ((ServiceServer) Objects.requireNonNull(serviceBox.getSelectedItem())).getName());
        });

        add(serviceBox);
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
