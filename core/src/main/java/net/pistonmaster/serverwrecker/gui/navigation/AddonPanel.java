package net.pistonmaster.serverwrecker.gui.navigation;

import java.awt.*;

public class AddonPanel extends NavigationItem {
    public AddonPanel() {
        super();

        setLayout(new GridLayout(5, 6, 5, 5));
    }

    @Override
    public String getNavigationName() {
        return "Addons";
    }

    @Override
    public String getRightPanelContainerConstant() {
        return RightPanelContainer.ADDON_MENU;
    }
}
