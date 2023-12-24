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
package net.pistonmaster.serverwrecker.client.gui.popups;

import net.pistonmaster.serverwrecker.builddata.BuildData;
import net.pistonmaster.serverwrecker.client.gui.libs.SwingTextUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class AboutPopup extends JPopupMenu {
    public AboutPopup() {
        add(SwingTextUtils.createHtmlPane("<b>ServerWrecker</b>"));
        add(SwingTextUtils.createHtmlPane("Version: <b><code>" + BuildData.VERSION + "</code></b>"));
        add(SwingTextUtils.createHtmlPane("Author: <b><a href='https://github.com/AlexProgrammerDE'>AlexProgrammerDE</a></b>"));
        add(SwingTextUtils.createHtmlPane("GitHub: <b><a href='https://github.com/AlexProgrammerDE/ServerWrecker'>github.com/AlexProgrammerDE/ServerWrecker</a></b>"));
        add(SwingTextUtils.createHtmlPane("Commit: <b><code>" + BuildData.COMMIT + "</code></b> " +
                "(<b><a href='https://github.com/AlexProgrammerDE/ServerWrecker/commit/" + BuildData.COMMIT + "'>Click to show</a></b>)"));
        setBorder(new EmptyBorder(10, 10, 10, 10));
    }
}
