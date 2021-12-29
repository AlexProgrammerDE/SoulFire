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

import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.gui.navigation.RightPanelContainer;

import javax.swing.*;
import java.awt.*;

public class AuthPanel extends JPanel {
    public AuthPanel(RightPanelContainer container) {
        super();

        setLayout(new GridBagLayout());

        JPanel centerPanel = new JPanel();

        JTextField email = new JTextField();
        JPasswordField password = new JPasswordField();

        email.setBorder(BorderFactory.createTitledBorder("Email"));
        password.setBorder(BorderFactory.createTitledBorder("Password"));

        email.setSize(new Dimension(200, 30));
        password.setSize(new Dimension(200, 30));

        JPanel credPanel = new JPanel();

        credPanel.add(email);
        credPanel.add(password);

        credPanel.setBackground(Color.CYAN);

        credPanel.setSize(new Dimension(200, 150));

        centerPanel.setBackground(Color.RED);

        centerPanel.setPreferredSize(new Dimension(500, 300));

        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(new JLabel(ServerWrecker.PROJECT_NAME), BorderLayout.NORTH);
        centerPanel.add(credPanel, BorderLayout.NORTH);

        centerPanel.setBorder(BorderFactory.createTitledBorder("Login"));

        add(centerPanel);
    }
}
