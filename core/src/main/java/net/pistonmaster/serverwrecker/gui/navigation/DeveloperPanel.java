package net.pistonmaster.serverwrecker.gui.navigation;

import javax.swing.*;
import java.awt.*;

public class DeveloperPanel extends NavigationItem {
    public static final JCheckBox debug = new JCheckBox();

    public DeveloperPanel() {
        super();

        setLayout(new GridLayout(0, 2));

        add(new JLabel("Debug: "));
        add(debug);
    }

    @Override
    public String getNavigationName() {
        return "Developer Tools";
    }

    @Override
    public String getRightPanelContainerConstant() {
        return RightPanelContainer.DEV_MENU;
    }
}
