package net.pistonmaster.wirebot.gui.navigation;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NavigationPanel extends JPanel {
    @Getter
    private final List<NavigationItem> navigationItemList = new ArrayList<>();

    public NavigationPanel(RightPanelContainer container) {
        super();

        setLayout(new GridLayout(3, 3, 10, 10));

        for (NavigationItem item : container.getPanels()) {
            JButton button = new JButton(item.getNavigationName());
            button.addActionListener(action -> {
                ((CardLayout) container.getLayout()).show(container, item.getRightPanelContainerConstant());
            });

            button.setSize(new Dimension(50, 50));

            add(button);
        }
    }
}
