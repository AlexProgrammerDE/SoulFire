/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.soulfire.client.gui;

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.client.gui.libs.MessageLogPanel;
import net.pistonmaster.soulfire.client.gui.libs.SwingTextUtils;
import net.pistonmaster.soulfire.grpc.generated.LogRequest;
import net.pistonmaster.soulfire.grpc.generated.LogResponse;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
public class LogPanel extends JPanel {
    private final MessageLogPanel messageLogPanel = new MessageLogPanel(3000);
    private final GUIManager guiManager;

    @Inject
    public LogPanel(GUIManager guiManager) {
        this.guiManager = guiManager;

        var request = LogRequest.newBuilder().setPrevious(300).build();
        guiManager.rpcClient().logStub().subscribe(request, new StreamObserver<>() {
            @Override
            public void onNext(LogResponse value) {
                messageLogPanel.log(value.getMessage() + "\n");
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error while logging!", t);
            }

            @Override
            public void onCompleted() {
            }
        });

        var commands = new JTextField();
        commands.putClientProperty("JTextField.placeholderText", "Type SoulFire commands here...");

        putClientProperty("log-panel-command-input", commands);

        var undoManager = SwingTextUtils.addUndoRedo(commands);

        var commandShellAction = new CommandShellAction(undoManager);
        commandShellAction.initHistory();

        commands.addActionListener(commandShellAction);
        commands.addKeyListener(new CommandShellKeyAdapter(undoManager, commandShellAction, commands));

        setLayout(new BorderLayout());
        add(messageLogPanel, BorderLayout.CENTER);
        add(commands, BorderLayout.SOUTH);

        setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 0));
    }

    @Getter
    @RequiredArgsConstructor
    private class CommandShellAction extends AbstractAction {
        private final UndoManager undoManager;
        private final List<String> commandHistory = new ArrayList<>();
        @Setter
        private int pointer = -1;

        public void initHistory() {
            commandHistory.addAll(guiManager.clientCommandManager().getCommandHistory()
                    .stream().map(Map.Entry::getValue).toList());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            pointer = -1;

            var command = e.getActionCommand();
            command = command.strip();

            if (command.isBlank()) {
                return;
            }

            ((JTextField) e.getSource()).setText(null);
            undoManager.discardAllEdits();

            commandHistory.add(command);
            guiManager.clientCommandManager().execute(command);
        }
    }

    @RequiredArgsConstructor
    private class CommandShellKeyAdapter extends KeyAdapter {
        private final UndoManager undoManager;
        private final CommandShellAction commandShellAction;
        private final JTextField commands;
        private String cachedText = null;

        @Override
        public void keyPressed(KeyEvent e) {
            // Cache the written text so we can restore it later
            if (commandShellAction.pointer() == -1) {
                cachedText = commands.getText();
            }

            var commandHistory = commandShellAction.commandHistory();
            var pointer = commandShellAction.pointer();
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP -> {
                    if (pointer < commandHistory.size() - 1) {
                        commandShellAction.pointer(pointer + 1);
                        commands.setText(getTextAtPointer());
                        undoManager.discardAllEdits();
                    }
                }
                case KeyEvent.VK_DOWN -> {
                    if (pointer > -1) {
                        commandShellAction.pointer(pointer - 1);
                        commands.setText(getTextAtPointer());
                        undoManager.discardAllEdits();
                    }
                }
                case KeyEvent.VK_ENTER -> cachedText = null;
            }
        }

        private String getTextAtPointer() {
            var commandHistory = commandShellAction.commandHistory();
            var pointer = commandShellAction.pointer();
            if (pointer == -1) {
                return cachedText;
            } else {
                return commandHistory.get(commandHistory.size() - 1 - pointer);
            }
        }
    }
}
