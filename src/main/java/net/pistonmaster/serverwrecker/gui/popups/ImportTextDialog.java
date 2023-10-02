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
import net.pistonmaster.serverwrecker.gui.libs.SwingTextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class ImportTextDialog extends JPopupMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportTextDialog.class);

    public ImportTextDialog(String loadText, String typeText, ServerWrecker serverWrecker, GUIFrame frame, Consumer<String> consumer) {
        setBorder(new EmptyBorder(10, 10, 10, 10));

        var button = new JButton(SwingTextUtils.htmlCenterText(loadText));

        var chooser = new FileChooser();
        chooser.setInitialDirectory(Path.of(System.getProperty("user.dir")).toFile());
        chooser.setTitle(loadText);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(typeText, "*.txt"));

        button.addActionListener(new ImportFileListener(serverWrecker, frame, chooser, consumer));
    }

    private record ImportFileListener(ServerWrecker serverWrecker, GUIFrame frame,
                                      FileChooser chooser, Consumer<String> consumer) implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            var accountFile = JFXFileHelper.showOpenDialog(chooser);
            if (accountFile == null) {
                return;
            }

            LOGGER.info("Opening: {}", accountFile.getFileName());
            serverWrecker.getThreadPool().submit(() -> {
                try {
                    consumer.accept(Files.readString(accountFile));
                } catch (Throwable e) {
                    LOGGER.error("Failed to load accounts!", e);
                }
            });
        }
    }
}
