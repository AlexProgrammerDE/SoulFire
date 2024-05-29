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
package com.soulfiremc.client.gui.navigation;

import com.soulfiremc.client.gui.libs.SFSwingUtils;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class NavigationWrapper {
  private NavigationWrapper() {}

  public static JPanel createBackWrapper(
    CardsContainer container, String target, NavigationItem item) {
    var topBar = new JPanel(new BorderLayout());

    var back = new JButton("Back");
    SFSwingUtils.addButtonPressAction(back, () -> container.show(target));

    topBar.add(back, BorderLayout.PAGE_END);
    topBar.setSize(new Dimension(topBar.getWidth(), 20));
    topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

    var panel = new JPanel(new BorderLayout());

    panel.add(topBar, BorderLayout.NORTH);

    var scrollPane = new JScrollPane(item);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
  }
}
