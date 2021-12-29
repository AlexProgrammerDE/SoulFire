/*
 * ServerWrecker
 *
 * Copyright (C) 2021 ServerWrecker
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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ShellSender extends AbstractAction {
    @Getter
    private final CommandDispatcher<ShellSender> dispatcher = new CommandDispatcher<>();
    private final Logger logger;
    @Getter
    private final List<String> commandHistory = new ArrayList<>();
    @Getter
    @Setter
    private int pointer = -1;

    {
        dispatcher.register(LiteralArgumentBuilder.<ShellSender>literal("test").executes(c -> {
            sendMessage("test");
            return 1;
        }));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        pointer = -1;

        String command = e.getActionCommand();

        if (command.isEmpty())
            return;

        ((JTextField) e.getSource()).setText(null);

        commandHistory.add(command);
        try {
            dispatcher.execute(command, this);
        } catch (CommandSyntaxException commandSyntaxException) {
            logger.warn("Invalid command syntax");
        }
    }

    public void sendMessage(String message) {
        logger.info(message);
    }
}
