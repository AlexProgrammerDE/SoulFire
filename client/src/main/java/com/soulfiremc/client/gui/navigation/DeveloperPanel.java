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

import com.soulfiremc.client.gui.GUIManager;
import com.soulfiremc.util.BuiltinSettingsConstants;
import java.awt.GridBagLayout;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeveloperPanel extends NavigationItem {
  @Inject
  public DeveloperPanel(GUIManager guiManager, CardsContainer cardsContainer) {
    setLayout(new GridBagLayout());

    GeneratedPanel.addComponents(
      this,
      cardsContainer.getByNamespace(BuiltinSettingsConstants.DEV_SETTINGS_ID),
      guiManager.clientSettingsManager());
  }

  @Override
  public String getNavigationName() {
    return "Developer Tools";
  }

  @Override
  public String getNavigationId() {
    return "dev-menu";
  }
}
