/*
 * ServerWrecker
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
package net.pistonmaster.serverwrecker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.grpc.RPCClient;
import net.pistonmaster.serverwrecker.grpc.generated.CommandCompletionRequest;
import net.pistonmaster.serverwrecker.grpc.generated.CommandHistoryRequest;
import net.pistonmaster.serverwrecker.grpc.generated.CommandRequest;
import net.pistonmaster.serverwrecker.server.api.ConsoleSubject;
import net.pistonmaster.serverwrecker.server.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.server.api.event.lifecycle.DispatcherInitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ClientCommandManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCommandManager.class);
    @Getter
    private final CommandDispatcher<ConsoleSubject> dispatcher = new CommandDispatcher<>();
    private final RPCClient rpcClient;

    @PostConstruct
    public void postConstruct() {
        ServerWreckerAPI.postEvent(new DispatcherInitEvent(dispatcher));
    }

    public int execute(String command) {
        try {
            var parsed = dispatcher.parse(command, ConsoleSubject.INSTANCE);

            // We found no registered command, so forward to server.
            if (parsed.getExceptions().isEmpty()) {
                return rpcClient.commandStubBlocking().executeCommand(
                        CommandRequest.newBuilder().setCommand(command).build()
                ).getCode();
            }

            return dispatcher.execute(parsed);
        } catch (CommandSyntaxException e) {
            LOGGER.error("An error occurred while trying to execute a command.", e);
            return 1;
        }
    }

    public List<String> getCompletionSuggestions(String command) {
        var suggestions = new ArrayList<String>();
        try {
            var offers = rpcClient.commandStubBlocking().tabCompleteCommand(
                    CommandCompletionRequest.newBuilder().setCommand(command).build()
            ).getSuggestionsList();
            suggestions.addAll(offers);
        } catch (Exception e) {
            LOGGER.error("An error occurred while trying to perform tab completion.", e);
        }

        return suggestions;
    }

    public List<Map.Entry<Instant, String>> getCommandHistory() {
        List<Map.Entry<Instant, String>> history = new ArrayList<>();
        for (var entry : rpcClient.commandStubBlocking()
                .getCommandHistory(CommandHistoryRequest.newBuilder().build())
                .getEntriesList()) {
            history.add(Map.entry(Instant.ofEpochSecond(entry.getTimestamp()), entry.getCommand()));
        }

        return history;
    }

    private record HelpData(String command, String help) {
    }
}
