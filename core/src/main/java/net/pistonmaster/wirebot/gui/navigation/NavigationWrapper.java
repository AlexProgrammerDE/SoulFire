package net.pistonmaster.wirebot.gui.navigation;

import javax.swing.*;
import java.awt.*;

public class NavigationWrapper {
    public static JPanel createWrapper(RightPanelContainer container, NavigationItem item) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel topBar = new JPanel();
        topBar.setLayout(new BorderLayout());

        JButton back = new JButton("Back");

        back.addActionListener(action -> {
            ((CardLayout)container.getLayout()).show(container, RightPanelContainer.NAVIGATION_MENU);
        });

        topBar.add(back, BorderLayout.PAGE_END);

        topBar.setSize(new Dimension(topBar.getWidth(), 20));

        panel.add(topBar, BorderLayout.NORTH);

        panel.add(item, BorderLayout.CENTER);

        return panel;
    }
}
