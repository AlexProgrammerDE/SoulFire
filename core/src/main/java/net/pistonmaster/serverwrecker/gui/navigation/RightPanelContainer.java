package net.pistonmaster.serverwrecker.gui.navigation;

import lombok.Getter;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.gui.AuthPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RightPanelContainer extends JPanel {
    public static final String AUTH_MENU = "AuthMenu";
    public static final String NAVIGATION_MENU = "NavigationMenu";
    public static final String SETTINGS_MENU = "SettingsMenu";
    public static final String ADDON_MENU = "AddonMenu";
    public static final String ACCOUNT_MENU = "AccountMenu";
    public static final String DEV_MENU = "DeveloperMenu";
    @Getter
    private final List<NavigationItem> panels = new ArrayList<>();

    public RightPanelContainer(ServerWrecker wireBot, JFrame parent) {
        super();

        panels.add(new SettingsPanel(wireBot));
        panels.add(new AddonPanel());
        panels.add(new AccountPanel(wireBot, parent));
        panels.add(new DeveloperPanel());

        setLayout(new CardLayout());

        NavigationPanel navigationPanel = new NavigationPanel(this);
        add(navigationPanel, NAVIGATION_MENU);

        AuthPanel authPanel = new AuthPanel(this);
        add(authPanel, AUTH_MENU);

        for (NavigationItem item : panels) {
            add(NavigationWrapper.createWrapper(this, item), item.getRightPanelContainerConstant());
        }

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public <A> A getPanel(Class<A> aClass) {
        return panels.stream().filter(aClass::isInstance).map(aClass::cast).findFirst().orElse(null);
    }
}
