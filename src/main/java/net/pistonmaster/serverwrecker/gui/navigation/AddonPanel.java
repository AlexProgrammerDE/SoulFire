/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
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
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.lifecycle.AddonPanelInitEvent;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AddonPanel extends NavigationItem {
    @Getter
    private final List<NavigationItem> navigationItems = new ArrayList<>();

    @Inject
    public AddonPanel(ButtonPanelContainer container) {
        ServerWreckerAPI.postEvent(new AddonPanelInitEvent(navigationItems));

        setLayout(new GridLayout(3, 3, 10, 10));

        for (NavigationItem item : navigationItems) {
            JButton button = new JButton(item.getNavigationName());
            button.addActionListener(action -> {
                ((CardLayout) container.getLayout()).show(container, item.getNavigationId());
            });

            button.setSize(new Dimension(50, 50));

            add(button);
        }
    }

    @Override
    public String getNavigationName() {
        return "Addons";
    }

    @Override
    public String getNavigationId() {
        return "addon-menu";
    }
}
