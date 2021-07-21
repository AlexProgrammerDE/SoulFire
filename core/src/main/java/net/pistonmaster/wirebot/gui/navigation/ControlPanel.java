package net.pistonmaster.wirebot.gui.navigation;

public class ControlPanel extends NavigationItem {
    @Override
    public String getNavigationName() {
        return "Controls";
    }

    @Override
    public String getRightPanelContainerConstant() {
        return RightPanelContainer.CONTROL_MENU;
    }
}
