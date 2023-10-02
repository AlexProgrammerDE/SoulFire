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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.pistonmaster.serverwrecker.grpc.generated.*;
import net.pistonmaster.serverwrecker.gui.libs.MessageLogPanel;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

@Getter
public class LogPanel extends JPanel {
    private final MessageLogPanel messageLogPanel = new MessageLogPanel(3000);
    private final GUIManager guiManager;

    @Inject
    public LogPanel(GUIManager guiManager) {
        this.guiManager = guiManager;

        var request = LogRequest.newBuilder().setPrevious(300).build();
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

        var commands = new JTextField();

        // commands.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.emptySet());

        var commandShellAction = new CommandShellAction();
        commandShellAction.initHistory();

        commands.addActionListener(commandShellAction);
        commands.addKeyListener(new CommandShellKeyAdapter(commandShellAction, commands));

        commands.putClientProperty("JTextField.placeholderText", "Type ServerWrecker commands here...");

        setLayout(new BorderLayout());
        add(messageLogPanel, BorderLayout.CENTER);
        add(commands, BorderLayout.SOUTH);

        setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 0));
    }

    @Getter
    private class CommandShellAction extends AbstractAction {
        private final List<String> commandHistory = new ArrayList<>();
        @Setter
        private int pointer = -1;

        public void initHistory() {
            var response = guiManager.getRpcClient().getCommandStubBlocking().getCommandHistory(CommandHistoryRequest.newBuilder().build());
            commandHistory.addAll(response.getCommandList());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            pointer = -1;

            var command = e.getActionCommand();

            if (command.isEmpty()) {
                return;
            }

            ((JTextField) e.getSource()).setText(null);

            commandHistory.add(command);
            guiManager.getRpcClient().getCommandStub().executeCommand(CommandRequest.newBuilder().setCommand(command).build(), new StreamObserver<>() {
                @Override
                public void onNext(CommandResponse value) {
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                }

                @Override
                public void onCompleted() {
                }
            });
        }
    }

    @RequiredArgsConstructor
    private class CommandShellKeyAdapter extends KeyAdapter {
        private final CommandShellAction commandShellAction;
        private final JTextField commands;
        private String cachedText = null;

        @Override
        public void keyPressed(KeyEvent e) {
            if (commandShellAction.getPointer() == -1) {
                cachedText = commands.getText();
            }

            var pointer = commandShellAction.getPointer();
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP -> {
                    if (pointer < commandShellAction.getCommandHistory().size() - 1) {
                        commandShellAction.setPointer(pointer + 1);
                        commands.setText(commandShellAction.getCommandHistory().get(commandShellAction.getPointer()));
                    }
                }
                case KeyEvent.VK_DOWN -> {
                    if (pointer > -1) {
                        commandShellAction.setPointer(pointer - 1);

                        if (commandShellAction.getPointer() == -1) {
                            commands.setText(cachedText);
                        } else {
                            commands.setText(commandShellAction.getCommandHistory().get(commandShellAction.getPointer()));
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
                        break;
                */
            }
        }
    }
}
