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
package net.pistonmaster.serverwrecker.gui.popups;

import javafx.stage.FileChooser;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.gui.GUIFrame;
import net.pistonmaster.serverwrecker.gui.libs.JFXFileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public class ImportTextDialog extends JDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportTextDialog.class);

    public ImportTextDialog(Path initialDirectory, String loadText, String typeText, ServerWrecker serverWrecker, GUIFrame frame, Consumer<String> consumer) {
        super(frame, loadText, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        var contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        var loadFromFileButton = new JButton("Load from File");

        var chooser = new FileChooser();
        chooser.setInitialDirectory(initialDirectory.toFile());
        chooser.setTitle(loadText);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(typeText, "*.txt"));

        loadFromFileButton.addActionListener(new ImportFileListener(serverWrecker, frame, chooser, consumer, this));

        var getFromClipboardButton = new JButton("Get from Clipboard");

        getFromClipboardButton.addActionListener(new ImportClipboardListener(serverWrecker, frame, chooser, consumer, this));

        buttonPanel.add(loadFromFileButton);
        buttonPanel.add(getFromClipboardButton);

        var textArea = new JTextArea(5, 20);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        var textScrollPane = new JScrollPane(textArea);
        var submitButton = new JButton("Submit");

        submitButton.addActionListener(new SubmitTextListener(serverWrecker, frame, chooser, consumer, this, textArea));

        var inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(textScrollPane, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);

        contentPane.add(buttonPanel, BorderLayout.NORTH);
        contentPane.add(inputPanel, BorderLayout.CENTER);

        setContentPane(contentPane);

        pack();
        setLocationRelativeTo(frame);
        setVisible(true);
    }

    private static Optional<String> getClipboard() {
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        var contents = clipboard.getContents(null);

        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                return ((String) contents.getTransferData(DataFlavor.stringFlavor)).describeConstable();
            } catch (UnsupportedFlavorException | IOException e) {
                LOGGER.error("Failed to get clipboard!", e);
                return Optional.empty();
            }
        } else {
            LOGGER.error("Clipboard does not contain text!");
            return Optional.empty();
        }
    }

    private record ImportFileListener(ServerWrecker serverWrecker, GUIFrame frame,
                                      FileChooser chooser, Consumer<String> consumer,
                                      ImportTextDialog dialog) implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            dialog.dispose();

            JFXFileHelper.showOpenDialog(chooser).thenAcceptAsync(file -> {
                if (file == null) {
                    return;
                }

                if (!Files.isReadable(file)) {
                    LOGGER.error("File is not readable!");
                    return;
                }

                LOGGER.info("Opening: {}", file.getFileName());

                try {
                    consumer.accept(Files.readString(file));
                } catch (Throwable e) {
                    LOGGER.error("Failed to import text!", e);
                }
            }, serverWrecker.getThreadPool());
        }
    }

    private record ImportClipboardListener(ServerWrecker serverWrecker, GUIFrame frame,
                                           FileChooser chooser, Consumer<String> consumer,
                                           ImportTextDialog dialog) implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            dialog.dispose();

            serverWrecker.getThreadPool().submit(() -> {
                try {
                    getClipboard().ifPresent(consumer);
                } catch (Throwable e) {
                    LOGGER.error("Failed to import text!", e);
                }
            });
        }
    }

    private record SubmitTextListener(ServerWrecker serverWrecker, GUIFrame frame,
                                      FileChooser chooser, Consumer<String> consumer,
                                      ImportTextDialog dialog, JTextArea textArea) implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            dialog.dispose();

            serverWrecker.getThreadPool().submit(() -> {
                try {
                    consumer.accept(textArea.getText());
                } catch (Throwable e) {
                    LOGGER.error("Failed to import text!", e);
                }
            });
        }
    }
}
