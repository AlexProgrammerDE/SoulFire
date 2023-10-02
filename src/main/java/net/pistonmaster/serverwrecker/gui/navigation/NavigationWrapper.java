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

import javax.swing.*;
import java.awt.*;

public class NavigationWrapper {
    private NavigationWrapper() {
    }

    public static JPanel createBackWrapper(CardsContainer container, String target, NavigationItem item) {
        var panel = new JPanel();
        panel.setLayout(new BorderLayout());

        var topBar = new JPanel();
        topBar.setLayout(new BorderLayout());

        var back = new JButton("Back");
        var cardLayout = (CardLayout) container.getLayout();
        back.addActionListener(action -> cardLayout.show(container, target));

        topBar.add(back, BorderLayout.PAGE_END);
        topBar.setSize(new Dimension(topBar.getWidth(), 20));
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(item, BorderLayout.CENTER);

        return panel;
    }
}
