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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.pistonmaster.serverwrecker.logging.CommandManager;

import com.google.inject.Inject;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ShellSender extends AbstractAction {
    private final CommandManager commandManager;
    @Getter
    private final List<String> commandHistory = new ArrayList<>();
    @Getter
    @Setter
    private int pointer = -1;

    @Override
    public void actionPerformed(ActionEvent e) {
        pointer = -1;

        String command = e.getActionCommand();

        if (command.isEmpty())
            return;

        ((JTextField) e.getSource()).setText(null);

        commandHistory.add(command);
        commandManager.execute(command);
    }
}
