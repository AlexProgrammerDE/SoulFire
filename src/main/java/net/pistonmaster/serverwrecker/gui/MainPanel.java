/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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
import ch.qos.logback.classic.Logger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.gui.libs.GhostText;
import net.pistonmaster.serverwrecker.gui.libs.MessageLogPanel;
import net.pistonmaster.serverwrecker.gui.navigation.RightPanelContainer;
import net.pistonmaster.serverwrecker.logging.LogAppender;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MainPanel extends JPanel {
    @Getter
    private static final MessageLogPanel logPanel = new MessageLogPanel(3000, false);
    private final ServerWrecker botManager;
    private final ShellSender shellSender;
    private final JFrame parent;
    private final Injector injector;
    private final RightPanelContainer rightPanelContainer;

    @PostConstruct
    public void postConstruct() {
        JPanel leftPanel = setLogPane();
        rightPanelContainer.create();

        setLayout(new BorderLayout());
        add(leftPanel, BorderLayout.CENTER);
        add(rightPanelContainer, BorderLayout.EAST);
    }

    private JPanel setLogPane() throws SecurityException {
        JPanel leftPanel = new JPanel();

        LogAppender logAppender = new LogAppender(logPanel);
        injector.register(LogAppender.class, logAppender);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(logAppender);

        JTextField commands = new JTextField();

        // commands.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.emptySet());

        commands.addActionListener(shellSender);
        commands.addKeyListener(new KeyAdapter() {
            private String cachedText = null;

            @Override
            public void keyPressed(KeyEvent e) {
                if (shellSender.getPointer() == -1) {
                    cachedText = commands.getText();
                }

                int pointer = shellSender.getPointer();
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> {
                        if (pointer < shellSender.getCommandHistory().size() - 1) {
                            shellSender.setPointer(pointer + 1);
                            commands.setText(shellSender.getCommandHistory().get(shellSender.getPointer()));
                        }
                    }
                    case KeyEvent.VK_DOWN -> {
                        if (pointer > -1) {
                            shellSender.setPointer(pointer - 1);

                            if (shellSender.getPointer() == -1) {
                                commands.setText(cachedText);
                            } else {
                                commands.setText(shellSender.getCommandHistory().get(shellSender.getPointer()));
                            }
                        } else {
                            commands.setText(cachedText);
                        }
                    }
                    case KeyEvent.VK_ENTER -> cachedText = null;

                        /*
                    case KeyEvent.VK_TAB:
                        e.consume();
                        ParseResults<ShellSender> results = shellSender.getDispatcher().parse(commands.getText(), shellSender);

                        System.out.println(results.getContext().findSuggestionContext(commands.getCaretPosition()).startPos);
                        System.out.println(results.getContext().findSuggestionContext(commands.getCaretPosition()).parent.getName());
                        break;*/
                }
            }
        });

        new GhostText(commands, "Type ServerWrecker commands here...");

        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(logPanel, BorderLayout.CENTER);
        leftPanel.add(commands, BorderLayout.SOUTH);

        return leftPanel;
    }
}
