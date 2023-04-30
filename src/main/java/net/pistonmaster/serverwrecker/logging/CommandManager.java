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
package net.pistonmaster.serverwrecker.logging;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.gui.MainPanel;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CommandManager {
    @Getter
    private final CommandDispatcher<ConsoleSubject> dispatcher = new CommandDispatcher<>();
    private final ServerWrecker serverWrecker;
    private final ConsoleSubject consoleSubject;

    @PostConstruct
    public void postConstruct() {
        dispatcher.register(LiteralArgumentBuilder.<ConsoleSubject>literal("online").executes(c -> {
            List<String> online = new ArrayList<>();
            serverWrecker.getBotConnections().forEach(client -> {
                if (client.isOnline()) {
                    online.add(client.protocol().getProfile().getName());
                }
            });
            c.getSource().sendMessage(online.size() + " bots online: " + String.join(", ", online));
            return 1;
        }));
        dispatcher.register(LiteralArgumentBuilder.<ConsoleSubject>literal("clear").executes(c -> {
            MainPanel.getLogPanel().clear();
            return 1;
        }));
        dispatcher.register(LiteralArgumentBuilder.<ConsoleSubject>literal("say")
                .then(RequiredArgumentBuilder.<ConsoleSubject, String>argument("message", StringArgumentType.greedyString())
                        .executes(c -> {
                            String message = StringArgumentType.getString(c, "message");
                            serverWrecker.getBotConnections().forEach(client -> {
                                if (client.isOnline()) {
                                    client.sendMessage(message);
                                }
                            });
                            return 1;
                        })));
        dispatcher.register(LiteralArgumentBuilder.<ConsoleSubject>literal("help")
                .executes(c -> {
                    c.getSource().sendMessage("Available commands:");
                    for (String command : dispatcher.getAllUsage(dispatcher.getRoot(), c.getSource(), false)) {
                        c.getSource().sendMessage(command);
                    }
                    return 1;
                }));
    }

    public void execute(String command) {
        try {
            dispatcher.execute(command, consoleSubject);
        } catch (CommandSyntaxException e) {
            serverWrecker.getLogger().warn(e.getMessage());
        }
    }

    public List<String> getCompletionSuggestions(String command) {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(command, consoleSubject)).join().getList()
                .stream().map(Suggestion::getText).toList();
    }
}
