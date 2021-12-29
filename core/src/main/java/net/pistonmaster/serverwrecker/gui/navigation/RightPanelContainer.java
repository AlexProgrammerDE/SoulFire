/*
 * ServerWrecker
 *
 * Copyright (C) 2021 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
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
