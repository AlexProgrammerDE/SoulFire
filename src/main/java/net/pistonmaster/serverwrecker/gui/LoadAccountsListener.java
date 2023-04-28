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

import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.ServerWrecker;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class LoadAccountsListener implements ActionListener {
    private final ServerWrecker serverWrecker;
    private final JFrame frame;
    private final JFileChooser fileChooser;

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path accountFile = fileChooser.getSelectedFile().toPath();
        serverWrecker.getLogger().info("Opening: {}.", accountFile.getFileName());

        serverWrecker.getThreadPool().submit(() -> {
            try (Stream<String> stream = Files.lines(accountFile)) {
                List<String> accounts = stream.distinct().collect(Collectors.toList());

                serverWrecker.getLogger().info("Loaded {} accounts", accounts.size());
                serverWrecker.setAccounts(accounts);
            } catch (Exception ex) {
                serverWrecker.getLogger().error(null, ex);
            }
        });
    }
}
