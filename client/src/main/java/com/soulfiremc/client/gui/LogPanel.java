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
package com.soulfiremc.client.gui;

import com.soulfiremc.brigadier.LocalConsole;
import com.soulfiremc.client.gui.libs.MessageLogPanel;
import com.soulfiremc.client.gui.libs.SFSwingUtils;
import com.soulfiremc.grpc.generated.LogRequest;
import io.grpc.Context;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.inject.Inject;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.undo.UndoManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class LogPanel extends JPanel {
  private final MessageLogPanel messageLogPanel = new MessageLogPanel(3000);
  private final GUIManager guiManager;

  @Inject
  public LogPanel(GUIManager guiManager) {
    this.guiManager = guiManager;

    var request = LogRequest.newBuilder().setPrevious(300).build();

    guiManager
      .threadPool()
      .submit(
        () -> {
          try (var context = Context.current().withCancellation()) {
            guiManager.rpcClient().contexts().add(context);
            context.run(
              () ->
                guiManager
                  .rpcClient()
                  .logStubBlocking()
                  .subscribe(request)
                  .forEachRemaining(
                    response -> messageLogPanel.log(response.getMessage() + "\n")));
          }
        });

    var commands = new JTextField();
    commands.setFocusTraversalKeysEnabled(false);

    commands.putClientProperty("JTextField.placeholderText", "Type SoulFire commands here...");

    putClientProperty("log-panel-command-input", commands);

    var undoManager = SFSwingUtils.addUndoRedo(commands);

    var commandShellAction = new CommandShellAction(undoManager, commands);
    commandShellAction.initHistory();

    commands.addActionListener(commandShellAction);
    commands.addKeyListener(commandShellAction);

    setLayout(new BorderLayout());
    add(messageLogPanel, BorderLayout.CENTER);
    add(commands, BorderLayout.SOUTH);

    setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 0));
  }

  @RequiredArgsConstructor
  private class CommandShellAction extends AbstractAction implements KeyListener {
    private final List<String> commandHistory = new ArrayList<>();
    private final UndoManager undoManager;
    private final JTextField commands;
    private Queue<String> tabQueue = null;
    private String cachedText = null;
    private int historyPointer = -1;

    public void initHistory() {
      commandHistory.addAll(
        guiManager.clientCommandManager().getCommandHistory().stream()
          .map(Map.Entry::getValue)
          .toList());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      historyPointer = -1;

      var command = e.getActionCommand();
      command = command.strip();

      if (command.isBlank()) {
        return;
      }

      ((JTextField) e.getSource()).setText(null);
      undoManager.discardAllEdits();

      commandHistory.add(command);
      guiManager.clientCommandManager().execute(command, new LocalConsole());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
      // Cache the written text so we can restore it later
      if (historyPointer == -1) {
        cachedText = commands.getText();
      }

      switch (e.getKeyCode()) {
        case KeyEvent.VK_UP -> {
          if (historyPointer < commandHistory.size() - 1) {
            historyPointer = historyPointer + 1;
            commands.setText(getTextAtPointer());
            undoManager.discardAllEdits();
          }
        }
        case KeyEvent.VK_DOWN -> {
          if (historyPointer > -1) {
            historyPointer = historyPointer - 1;
            commands.setText(getTextAtPointer());
            undoManager.discardAllEdits();
          }
        }
        case KeyEvent.VK_ENTER -> cachedText = null;
        case KeyEvent.VK_TAB -> {
          e.consume();
          var caretPos = commands.getCaretPosition();
          var textLength = commands.getText().length();

          // If caret is not at the end of the text, we don't want to autocomplete
          if (caretPos != textLength) {
            log.warn("Caret is not at the end of the text, not autocompleting!");
            return;
          }

          var command = commands.getText();
          if (tabQueue == null) {
            tabQueue =
              new LinkedBlockingQueue<>(
                guiManager.clientCommandManager().getCompletionSuggestions(command, new LocalConsole()));
          }

          if (tabQueue.isEmpty()) {
            return;
          }

          var suggestion = tabQueue.poll();
          tabQueue.add(suggestion);

          var split = command.split(" ", -1);

          // Change command by inserting the suggestion as the last "word"
          // Word may be "" to allow inserting into the end of the string
          var finalCommand = new String[split.length];
          System.arraycopy(split, 0, finalCommand, 0, split.length - 1);
          finalCommand[split.length - 1] = suggestion;

          commands.setText(String.join(" ", finalCommand));
        }
        default -> tabQueue = null;
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    private String getTextAtPointer() {
      if (historyPointer == -1) {
        return cachedText;
      } else {
        return commandHistory.get(commandHistory.size() - 1 - historyPointer);
      }
    }
  }
}
