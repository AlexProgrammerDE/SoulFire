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
package net.pistonmaster.serverwrecker.gui.popups;

import net.pistonmaster.serverwrecker.builddata.BuildData;
import org.intellij.lang.annotations.Language;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class AboutPopup extends JPopupMenu {
    public AboutPopup() {
        add(createPane("<b>ServerWrecker</b>"));
        add(createPane("Version: <b><code>" + BuildData.VERSION + "</code></b>"));
        add(createPane("Author: <b><a href='https://github.com/AlexProgrammerDE'>AlexProgrammerDE</a></b>"));
        add(createPane("GitHub: <b><a href='" + BuildData.URL + "'>" + BuildData.URL + "</a></b>"));
        add(createPane("Commit: <b><code>" + BuildData.COMMIT + "</code></b>"));
        setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    private JTextPane createPane(@Language("html") String text) {
        JTextPane pane = new JTextPane();
        pane.setContentType("text/html");
        pane.setText(text);
        pane.setEditable(false);
        pane.setBackground(null);
        pane.setBorder(null);
        return pane;
    }
}
