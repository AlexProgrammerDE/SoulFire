/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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

import javax.swing.*;
import java.awt.*;

public class NavigationWrapper {
    public static JPanel createWrapper(RightPanelContainer container, NavigationItem item) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel topBar = new JPanel();
        topBar.setLayout(new BorderLayout());

        JButton back = new JButton("Back");

        back.addActionListener(action -> {
            ((CardLayout) container.getLayout()).show(container, RightPanelContainer.NAVIGATION_MENU);
        });

        topBar.add(back, BorderLayout.PAGE_END);

        topBar.setSize(new Dimension(topBar.getWidth(), 20));

        topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        panel.add(topBar, BorderLayout.NORTH);

        panel.add(item, BorderLayout.CENTER);

        return panel;
    }


}
