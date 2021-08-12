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
