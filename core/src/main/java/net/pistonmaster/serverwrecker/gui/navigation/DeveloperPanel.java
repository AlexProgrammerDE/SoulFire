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

import ch.qos.logback.classic.Level;
import net.pistonmaster.serverwrecker.ServerWrecker;

import javax.swing.*;
import java.awt.*;

public class DeveloperPanel extends NavigationItem {
    public static final JCheckBox debug = new JCheckBox();

    public DeveloperPanel() {
        super();

        setLayout(new GridLayout(0, 2));

        add(new JLabel("Debug: "));
        add(debug);

        debug.addActionListener(listener -> {
            if (debug.isSelected()) {
                ServerWrecker.getInstance().setupLogging(Level.DEBUG);
            } else {
                ServerWrecker.getInstance().setupLogging(Level.INFO);
            }
        });
    }

    @Override
    public String getNavigationName() {
        return "Developer Tools";
    }

    @Override
    public String getRightPanelContainerConstant() {
        return RightPanelContainer.DEV_MENU;
    }
}
