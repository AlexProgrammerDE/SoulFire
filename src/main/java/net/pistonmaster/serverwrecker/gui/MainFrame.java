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
package net.pistonmaster.serverwrecker.gui;

import ch.jalu.injector.Injector;
import com.formdev.flatlaf.util.SystemInfo;
import javafx.embed.swing.JFXPanel;
import net.pistonmaster.serverwrecker.ServerWrecker;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class MainFrame extends JFrame {
    public static final String MAIN_MENU = "MainMenu";
    private final ServerWrecker serverWrecker;
    private final Injector injector;

    @Inject
    public MainFrame(ServerWrecker serverWrecker, Injector injector) {
        super("ServerWrecker");
        this.serverWrecker = serverWrecker;
        this.injector = injector;
        injector.register(JFrame.class, this);
        setAppTitle();
    }

    @PostConstruct
    public void postConstruct() {
        new JFXPanel(); // Initializes the JavaFX Platform

        if (SystemInfo.isMacOS) {
            // Hide window title because we want to avoid dark-mode name issues
            getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }

        setResizable(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setJMenuBar(injector.getSingleton(SWMenuBar.class));

        setLayout(new CardLayout());
        add(injector.getSingleton(MainPanel.class), MAIN_MENU);

        pack();

        setMinimumSize(new Dimension(600, 400));

        serverWrecker.getLogger().info("Opening GUI!");

        setVisible(true);
    }

    public void setAppTitle() {
        try {
            Toolkit xToolkit = Toolkit.getDefaultToolkit();
            if (!xToolkit.getClass().getName().equals("sun.awt.X11.XToolkit")) {
                return;
            }

            VarHandle CLASS_NAME_VARIABLE = MethodHandles
                    .privateLookupIn(xToolkit.getClass(), MethodHandles.lookup())
                    .findStaticVarHandle(xToolkit.getClass(), "awtAppClassName", String.class);

            CLASS_NAME_VARIABLE.set("ServerWrecker");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
