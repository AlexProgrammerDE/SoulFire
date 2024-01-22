/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.soulfire.client.gui.navigation;

import ch.jalu.injector.Injector;
import net.pistonmaster.soulfire.client.gui.libs.SwingTextUtils;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

public class NavigationPanel extends JPanel {
    public static final String NAVIGATION_ID = "navigation-menu";

    @Inject
    public NavigationPanel(CardsContainer container, Injector injector) {
        setLayout(new GridLayout(0, 2, 10, 10));

        for (var item : container.panels()) {
            var button = new JButton(SwingTextUtils.htmlCenterText(item.getNavigationName()));

            button.addActionListener(action -> container.show(item.getNavigationId()));
            container.putClientProperty(item.getNavigationId() + "-button", button);

            add(button);
        }

        add(injector.getSingleton(ControlPanel.class));
    }
}
