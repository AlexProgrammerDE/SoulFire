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
package net.pistonmaster.serverwrecker.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import net.pistonmaster.serverwrecker.ServerWrecker;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    public static final String AUTH_MENU = "AuthMenu";
    public static final String MAIN_MENU = "MainMenu";

    public MainFrame(ServerWrecker botManager) {
        super(ServerWrecker.PROJECT_NAME);

        setLookAndFeel();
        setResizable(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setLayout(new CardLayout());
        add(new MainPanel(botManager, this), MAIN_MENU);

        pack();

        setSize(new Dimension(getWidth() + 200, getHeight()));

        setVisible(true);

        ServerWrecker.getLogger().info("Started program");
    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception ex) {
            ServerWrecker.getLogger().error("Failed to initialize LaF");
        }
    }
}
