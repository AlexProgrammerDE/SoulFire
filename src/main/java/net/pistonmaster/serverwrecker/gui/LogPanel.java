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

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import net.pistonmaster.serverwrecker.grpc.generated.logs.LogRequest;
import net.pistonmaster.serverwrecker.grpc.generated.logs.LogResponse;
import net.pistonmaster.serverwrecker.gui.libs.MessageLogPanel;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

@Getter
public class LogPanel extends JPanel {
    private final MessageLogPanel messageLogPanel = new MessageLogPanel(3000);

    @Inject
    public LogPanel(ShellSender shellSender, GUIManager guiManager) {
        LogRequest request = LogRequest.newBuilder().setPrevious(300).build();
        guiManager.getRpcClient().getLogStub().subscribe(request, new StreamObserver<>() {
            @Override
            public void onNext(LogResponse value) {
                messageLogPanel.log(value.getMessage() + "\n");
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
            }
        });

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

        setLayout(new BorderLayout());
        add(messageLogPanel, BorderLayout.CENTER);
        add(commands, BorderLayout.SOUTH);

        setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 0));
    }
}
