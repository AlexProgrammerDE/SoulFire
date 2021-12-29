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

import java.awt.*;

public class AddonPanel extends NavigationItem {
    public AddonPanel() {
        super();

        setLayout(new GridLayout(5, 6, 5, 5));
    }

    @Override
    public String getNavigationName() {
        return "Addons";
    }

    @Override
    public String getRightPanelContainerConstant() {
        return RightPanelContainer.ADDON_MENU;
    }
}
