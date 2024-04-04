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
package com.soulfiremc.client.gui.popups;

import com.soulfiremc.client.gui.GUIFrame;
import com.soulfiremc.client.gui.GUIManager;
import com.soulfiremc.client.gui.libs.HintTextArea;
import com.soulfiremc.client.gui.libs.JFXFileHelper;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImportTextDialog extends JDialog {
  public ImportTextDialog(
    Path initialDirectory,
    String loadText,
    String typeText,
    GUIManager guiManager,
    GUIFrame frame,
    Consumer<String> consumer) {
    super(frame, loadText, true);
    Consumer<String> threadSpawningConsumer =
      text -> guiManager.threadPool().submit(() -> consumer.accept(text));

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    var contentPane = new JPanel(new BorderLayout());
    contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    var buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    var loadFromFileButton = new JButton("Load from File");

    loadFromFileButton.addActionListener(
      new ImportFileListener(
        initialDirectory, Map.of(typeText, "txt"), threadSpawningConsumer, this));

    var getFromClipboardButton = new JButton("Get from Clipboard");

    getFromClipboardButton.addActionListener(
      new ImportClipboardListener(threadSpawningConsumer, this));

    buttonPanel.add(loadFromFileButton);
    buttonPanel.add(getFromClipboardButton);

    var textArea = new HintTextArea("Put text here...", 5, 20);
    textArea.setWrapStyleWord(true);
    textArea.setLineWrap(true);

    var textScrollPane = new JScrollPane(textArea);
    var submitButton = new JButton("Submit");

    submitButton.addActionListener(new SubmitTextListener(threadSpawningConsumer, this, textArea));

    var inputPanel = new JPanel(new BorderLayout());
    inputPanel.add(textScrollPane, BorderLayout.CENTER);
    inputPanel.add(submitButton, BorderLayout.EAST);

    contentPane.add(buttonPanel, BorderLayout.NORTH);

    var separatorPanel = new JPanel();
    var titledBorder =
      BorderFactory.createTitledBorder(
        new MatteBorder(
          UIManager.getInt("Separator.stripeWidth"),
          0,
          0,
          0,
          UIManager.getColor("Separator.foreground")),
        "OR");
    titledBorder.setTitleJustification(TitledBorder.CENTER);
    separatorPanel.setBorder(titledBorder);
    var separatorPanelLayout = new GridBagLayout();
    separatorPanelLayout.columnWidths = new int[] {0, 0};
    separatorPanelLayout.rowHeights = new int[] {0, 0};
    separatorPanelLayout.columnWeights = new double[] {1.0, Double.MIN_VALUE};
    separatorPanelLayout.rowWeights = new double[] {1.0, Double.MIN_VALUE};
    separatorPanel.setLayout(separatorPanelLayout);
    contentPane.add(separatorPanel, BorderLayout.CENTER);

    contentPane.add(inputPanel, BorderLayout.SOUTH);

    setContentPane(contentPane);

    pack();
    setLocationRelativeTo(frame);
    setVisible(true);
  }

  private static Optional<String> getClipboard() {
    var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    var contents = clipboard.getContents(null);

    if (contents == null || !contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      log.error("Clipboard does not contain text!");
      return Optional.empty();
    }

    try {
      return ((String) contents.getTransferData(DataFlavor.stringFlavor)).describeConstable();
    } catch (UnsupportedFlavorException | IOException e) {
      log.error("Failed to get clipboard!", e);
      return Optional.empty();
    }
  }

  private record ImportFileListener(
    Path initialDirectory,
    Map<String, String> filterMap,
    Consumer<String> consumer,
    ImportTextDialog dialog)
    implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      dialog.dispose();

      JFXFileHelper.showOpenDialog(initialDirectory, filterMap)
        .ifPresent(
          file -> {
            if (!Files.isReadable(file)) {
              log.error("File is not readable!");
              return;
            }

            log.info("Opening: {}", file.getFileName());

            try {
              consumer.accept(Files.readString(file));
            } catch (Throwable e) {
              log.error("Failed to import text!", e);
            }
          });
    }
  }

  private record ImportClipboardListener(Consumer<String> consumer, ImportTextDialog dialog)
    implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      dialog.dispose();

      try {
        getClipboard().ifPresent(consumer);
      } catch (Throwable e) {
        log.error("Failed to import text!", e);
      }
    }
  }

  private record SubmitTextListener(
    Consumer<String> consumer, ImportTextDialog dialog, JTextArea textArea)
    implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      dialog.dispose();

      try {
        consumer.accept(textArea.getText());
      } catch (Throwable e) {
        log.error("Failed to import text!", e);
      }
    }
  }
}
