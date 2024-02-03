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
package net.pistonmaster.soulfire.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.brigadier.ConsoleSubject;
import net.pistonmaster.soulfire.client.grpc.RPCClient;
import net.pistonmaster.soulfire.client.settings.SettingsManager;
import net.pistonmaster.soulfire.grpc.generated.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.pistonmaster.soulfire.brigadier.BrigadierHelper.help;
import static net.pistonmaster.soulfire.brigadier.BrigadierHelper.literal;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ClientCommandManager {
    @Getter
    private final CommandDispatcher<ConsoleSubject> dispatcher = new CommandDispatcher<>();
    private final RPCClient rpcClient;
    private final SettingsManager settingsManager;

    @PostConstruct
    public void postConstruct() {
        dispatcher.register(literal("start-attack")
                .executes(help("Start a attack using the current settings", c -> {
                    rpcClient.attackStub().startAttack(AttackStartRequest.newBuilder()
                            .setSettings(settingsManager.exportSettings()).build(), new StreamObserver<>() {
                        @Override
                        public void onNext(AttackStartResponse value) {
                            log.debug("Started bot attack with id {}", value.getId());
                            // TODO: Sync with GUI state somehow
                        }

                        @Override
                        public void onError(Throwable t) {
                            log.error("Error while starting bot attack!", t);
                        }

                        @Override
                        public void onCompleted() {
                        }
                    });

                    return Command.SINGLE_SUCCESS;
                })));
    }

    public int execute(String command) {
        try {
            if (isClientCommand(command)) {
                log.debug("Executing command {} on client", command);
                return dispatcher.execute(command, ConsoleSubject.INSTANCE);
            } else {
                log.debug("Executing command {} on server", command);
                return rpcClient.commandStubBlocking().executeCommand(
                        CommandRequest.newBuilder().setCommand(command).build()
                ).getCode();
            }
        } catch (CommandSyntaxException e) {
            log.error("An error occurred while trying to execute a command.", e);
            return 1;
        }
    }

    private boolean isClientCommand(String command) {
        var spaceIndex = command.indexOf(' ');
        var commandName = spaceIndex == -1 ? command : command.substring(0, spaceIndex);
        return dispatcher.getRoot().getChild(commandName) != null;
    }

    public List<String> getCompletionSuggestions(String command) {
        try {
            return rpcClient.commandStubBlocking().tabCompleteCommand(
                    CommandCompletionRequest.newBuilder().setCommand(command).build()
            ).getSuggestionsList();
        } catch (Exception e) {
            log.error("An error occurred while trying to perform tab completion.", e);
            return List.of();
        }
    }

    public List<Map.Entry<Instant, String>> getCommandHistory() {
        var history = new ArrayList<Map.Entry<Instant, String>>();
        for (var entry : rpcClient.commandStubBlocking()
                .getCommandHistory(CommandHistoryRequest.newBuilder().build())
                .getEntriesList()) {
            history.add(Map.entry(Instant.ofEpochSecond(entry.getTimestamp()), entry.getCommand()));
        }

        return history;
    }
}
