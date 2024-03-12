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
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.inject.Inject;
import javax.swing.JButton;
import lombok.Getter;

@Getter
public class PluginListPanel extends NavigationItem {
  public static final String NAVIGATION_ID = "plugin-menu";

  @Inject
  public PluginListPanel(CardsContainer container) {
    setLayout(new GridLayout(0, 3, 10, 10));

    for (var item : container.pluginPages()) {
      if (item.getHidden()) {
        continue;
      }

      var button = new JButton(SFSwingUtils.htmlCenterText(item.getPageName()));

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
