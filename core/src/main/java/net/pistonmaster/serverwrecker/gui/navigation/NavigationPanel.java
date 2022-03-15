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

import lombok.Getter;
import net.pistonmaster.serverwrecker.ServerWrecker;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NavigationPanel extends JPanel {
    @Getter
    private final List<NavigationItem> navigationItemList = new ArrayList<>();

    public NavigationPanel(RightPanelContainer container) {
        super();

        setLayout(new GridLayout(3, 3, 10, 10));

        for (NavigationItem item : container.getPanels()) {
            JButton button = new JButton(item.getNavigationName());
            button.addActionListener(action -> {
                ((CardLayout) container.getLayout()).show(container, item.getRightPanelContainerConstant());
            });

            button.setSize(new Dimension(50, 50));

            add(button);
        }

        add(new ControlPanel(container, ServerWrecker.getInstance()));
    }
}
