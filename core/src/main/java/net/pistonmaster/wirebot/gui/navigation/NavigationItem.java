package net.pistonmaster.wirebot.gui.navigation;

import javax.swing.*;

public abstract class NavigationItem extends JPanel {
    public abstract String getNavigationName();

    public abstract String getRightPanelContainerConstant();
}
