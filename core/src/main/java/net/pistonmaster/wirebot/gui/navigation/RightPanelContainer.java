package net.pistonmaster.wirebot.gui.navigation;

import lombok.Getter;
import net.pistonmaster.wirebot.WireBot;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RightPanelContainer extends JPanel {
    public static final String NAVIGATION_MENU = "NavigationMenu";
    public static final String SETTINGS_MENU = "SettingsMenu";
    public static final String ADDON_MENU = "AddonMenu";
    public static final String ACCOUNT_MENU = "AccountMenu";
    public static final String CONTROL_MENU = "ControlMenu";
    @Getter
    private final List<NavigationItem> panels = new ArrayList<>();

    public RightPanelContainer(WireBot wireBot, JFrame parent) {
        super();

        panels.add(new SettingsPanel(wireBot));
        panels.add(new AddonPanel());
        panels.add(new AccountPanel(wireBot, parent));
        panels.add(new ControlPanel(this, wireBot));

        setLayout(new CardLayout());

        NavigationPanel navigationPanel = new NavigationPanel(this);
        add(navigationPanel, NAVIGATION_MENU);

        for (NavigationItem item : panels) {
            add(NavigationWrapper.createWrapper(this, item), item.getRightPanelContainerConstant());
        }

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public <A> A getPanel(Class<A> aClass) {
        return panels.stream().filter(aClass::isInstance).map(aClass::cast).findFirst().orElse(null);
    }
}
