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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.pistonmaster.serverwrecker.ServerWrecker;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ShellSender extends AbstractAction {
    private final ServerWrecker serverWrecker;
    @Getter
    private final CommandDispatcher<ShellSender> dispatcher = new CommandDispatcher<>();
    @Getter
    private final List<String> commandHistory = new ArrayList<>();
    @Getter
    @Setter
    private int pointer = -1;

    @PostConstruct
    public void postConstruct() {
        dispatcher.register(LiteralArgumentBuilder.<ShellSender>literal("test").executes(c -> {
            sendMessage("test");
            return 1;
        }));
        dispatcher.register(LiteralArgumentBuilder.<ShellSender>literal("online").executes(c -> {
            List<String> online = new ArrayList<>();
            serverWrecker.getBotConnections().forEach(client -> {
                if (client.isOnline()) {
                    online.add(client.protocol().getProfile().getName());
                }
            });
            sendMessage(online.size() + " bots online: " + String.join(", ", online));
            return 1;
        }));
        dispatcher.register(LiteralArgumentBuilder.<ShellSender>literal("clear").executes(c -> {
            MainPanel.getLogPanel().clear();
            return 1;
        }));
        dispatcher.register(LiteralArgumentBuilder.<ShellSender>literal("say")
                .then(RequiredArgumentBuilder.<ShellSender, String>argument("message", StringArgumentType.greedyString()).build())
                .executes(c -> {
                    String message = StringArgumentType.getString(c, "message");
                    serverWrecker.getBotConnections().forEach(client -> {
                        if (client.isOnline()) {
                            client.sendMessage(message);
                        }
                    });
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
            serverWrecker.getLogger().warn("Invalid command syntax");
        }
    }

    public void sendMessage(String message) {
        serverWrecker.getLogger().info(message);
    }
}
