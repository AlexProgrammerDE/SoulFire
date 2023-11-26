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
import net.pistonmaster.serverwrecker.gui.libs.SwingTextUtils;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

@Getter
public class PluginListPanel extends NavigationItem {
    public static final String NAVIGATION_ID = "plugin-menu";

    @Inject
    public PluginListPanel(CardsContainer container) {
        setLayout(new GridLayout(0, 3, 10, 10));

        for (var item : container.getPluginPages()) {
            if (item.getHidden()) {
                continue;
            }

            var button = new JButton(SwingTextUtils.htmlCenterText(item.getPageName()));

            button.addActionListener(action -> container.show(item.getNamespace()));
            button.setSize(new Dimension(50, 50));

            add(button);
        }
    }

    @Override
    public String getNavigationName() {
        return "Plugins";
    }

    @Override
    public String getNavigationId() {
        return NAVIGATION_ID;
    }
}
