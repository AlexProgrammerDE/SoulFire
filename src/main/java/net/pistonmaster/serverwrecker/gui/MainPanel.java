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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.gui.libs.MessageLogPanel;
import net.pistonmaster.serverwrecker.gui.navigation.CardsContainer;
import net.pistonmaster.serverwrecker.logging.LogAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MainPanel extends JPanel {
    @Getter
    private final MessageLogPanel messageLogPanel = new MessageLogPanel(3000);
    private final ShellSender shellSender;
    private final Injector injector;
    private final CardsContainer cardsContainer;

    @PostConstruct
    public void postConstruct() {
        JPanel logPanel = createLogPanel();
        cardsContainer.create();

        setLayout(new GridBagLayout());

        GridBagConstraints splitConstraints = new GridBagConstraints();
        splitConstraints.fill = GridBagConstraints.BOTH;
        splitConstraints.weightx = 1;
        splitConstraints.weighty = 1;

        cardsContainer.setMinimumSize(new Dimension(600, 0));
        logPanel.setMinimumSize(new Dimension(600, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, cardsContainer, logPanel);

        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(0.5d);
        splitPane.setContinuousLayout(true);

        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        add(splitPane, splitConstraints);
    }

    private JPanel createLogPanel() throws SecurityException {
        JPanel logPanel = new JPanel();

        LogAppender logAppender = new LogAppender(messageLogPanel);
        logAppender.start();
        injector.register(LogAppender.class, logAppender);
        ((Logger) LogManager.getRootLogger()).addAppender(logAppender);

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

        commands.putClientProperty("JTextField.placeholderText", "Type ServerWrecker commands here...");

        logPanel.setLayout(new BorderLayout());
        logPanel.add(messageLogPanel, BorderLayout.CENTER);
        logPanel.add(commands, BorderLayout.SOUTH);

        logPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 0));

        return logPanel;
    }
}
