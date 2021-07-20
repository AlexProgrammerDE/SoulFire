package net.pistonmaster.wirebot.gui;

import net.pistonmaster.wirebot.WireBot;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AuthPanel extends JPanel {
    private final WireBot botManager;
    private final JFrame parent;

    public AuthPanel(WireBot botManager, JFrame parent) {
        super();
        this.botManager = botManager;
        this.parent = parent;

        setLayout(new GridBagLayout());
        add(centerPanel());


    }

    private JPanel centerPanel() {
        JPanel centerPanel = new JPanel();

        JTextField email = new JTextField();
        JPasswordField password = new JPasswordField();

        email.setMinimumSize(new Dimension(email.getWidth() + 30, email.getHeight()));
        email.setBorder(BorderFactory.createTitledBorder("Email"));
        password.setBorder(BorderFactory.createTitledBorder("Password"));

        email.setMinimumSize(new Dimension(30, email.getHeight()));
        password.setMinimumSize(new Dimension(30, password.getHeight()));

        email.setBorder(new EmptyBorder(5, 5, 5, 5));
        password.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel credPanel = new JPanel();
        credPanel.setSize(new Dimension(50, email.getHeight() + password.getHeight() + 10));
        credPanel.setLayout(new BorderLayout());
        credPanel.add(email, BorderLayout.PAGE_START);
        credPanel.add(password, BorderLayout.PAGE_END);

        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(new JLabel(WireBot.PROJECT_NAME), BorderLayout.NORTH);
        centerPanel.add(credPanel, BorderLayout.SOUTH);

        centerPanel.setPreferredSize(new Dimension(400, 200));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Login"));

        return centerPanel;
    }
}
