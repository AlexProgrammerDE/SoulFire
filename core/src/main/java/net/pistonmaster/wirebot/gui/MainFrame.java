package net.pistonmaster.wirebot.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import net.pistonmaster.wirebot.WireBot;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    public static final String AUTH_MENU = "AuthMenu";
    public static final String MAIN_MENU = "MainMenu";
    private final WireBot botManager;

    public MainFrame(WireBot botManager) {
        super(WireBot.PROJECT_NAME);
        this.botManager = botManager;

        setLookAndFeel();
        setResizable(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setLayout(new CardLayout());
        add(new MainPanel(botManager, this), MAIN_MENU);

        pack();

        setSize(new Dimension(getWidth() + 200, getHeight()));

        setVisible(true);

        WireBot.getLogger().info("Started program");
    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
    }
}
