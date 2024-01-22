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
package net.pistonmaster.soulfire.client.gui.popups;

import net.pistonmaster.soulfire.builddata.BuildData;
import net.pistonmaster.soulfire.client.gui.libs.SwingTextUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class AboutPopup extends JDialog {
    public AboutPopup(JFrame parent) {
        super(parent, "About SoulFire", true);

        var content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(SwingTextUtils.createHtmlPane("<b>SoulFire</b>"));
        content.add(SwingTextUtils.createHtmlPane("Version: <b><code>" + BuildData.VERSION + "</code></b>"));
        content.add(SwingTextUtils.createHtmlPane("Author: <b><a href='https://github.com/AlexProgrammerDE'>AlexProgrammerDE</a></b>"));
        content.add(SwingTextUtils.createHtmlPane("GitHub: <b><a href='https://github.com/AlexProgrammerDE/SoulFire'>github.com/AlexProgrammerDE/SoulFire</a></b>"));
        content.add(SwingTextUtils.createHtmlPane("Commit: <b><code>" + BuildData.COMMIT.substring(0, 7) + "</code></b> " +
                "(<b><a href='https://github.com/AlexProgrammerDE/SoulFire/commit/" + BuildData.COMMIT + "'>Click to show</a></b>)"));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        add(content);

        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }
}
