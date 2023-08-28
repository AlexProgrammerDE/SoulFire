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
import lombok.Getter;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.grpc.RPCClient;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class GUIFrame extends JFrame {
    public static final String MAIN_MENU = "MainMenu";

    public GUIFrame() {
        super("ServerWrecker");
    }

    public void initComponents(Injector injector) {
        if (SystemInfo.isMacOS) {
            // Hide window title because we want to avoid dark-mode name issues
            getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }

        setResizable(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setJMenuBar(injector.getSingleton(SWMenuBar.class));

        setLayout(new CardLayout());
        add(injector.newInstance(MainPanel.class), MAIN_MENU);

        pack();

        // Calculate 16:9 width from height
        int height = getHeight();
        double aspectRatio = 16.0 / 9.0;
        int width = (int) (height * aspectRatio);

        setSize(width, height);
        setMinimumSize(new Dimension(width, height));
    }
}
