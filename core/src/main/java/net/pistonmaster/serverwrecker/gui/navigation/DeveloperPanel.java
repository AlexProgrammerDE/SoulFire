package net.pistonmaster.serverwrecker.gui.navigation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.pistonmaster.serverwrecker.ServerWrecker;
import org.slf4j.LoggerFactory;

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
                ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG);
                ((ch.qos.logback.classic.Logger) ServerWrecker.getLogger()).setLevel(Level.DEBUG);
            } else {
                ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
                ((ch.qos.logback.classic.Logger) ServerWrecker.getLogger()).setLevel(Level.INFO);
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
