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
