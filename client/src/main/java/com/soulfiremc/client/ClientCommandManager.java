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
package com.soulfiremc.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.soulfiremc.brigadier.ClientConsoleCommandSource;
import com.soulfiremc.brigadier.CommandSource;
import com.soulfiremc.brigadier.PlatformCommandManager;
import com.soulfiremc.client.grpc.RPCClient;
import com.soulfiremc.client.settings.ClientSettingsManager;
import com.soulfiremc.grpc.generated.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

import static com.soulfiremc.client.ClientBrigadierHelper.help;
import static com.soulfiremc.client.ClientBrigadierHelper.literal;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ClientCommandManager implements PlatformCommandManager<CommandSource> {
  @Getter
  private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
  private final RPCClient rpcClient;
  private final ClientSettingsManager clientSettingsManager;

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @PostConstruct
  public void postConstruct() {
    dispatcher.register(
      literal("start-attack")
        .executes(
          help(
            "Start a attack using the current settings",
            c -> {
              var cliInstance = rpcClient.instanceStubBlocking()
                .listInstances(InstanceListRequest.newBuilder().build())
                .getInstancesList()
                .stream()
                .filter(instance -> instance.getFriendlyName().equals("cli-attack"))
                .map(InstanceListResponse.Instance::getId)
                .map(UUID::fromString)
                .findFirst();

              var cliInstanceId = cliInstance.orElseGet(() -> UUID.fromString(rpcClient.instanceStubBlocking()
                .createInstance(
                  InstanceCreateRequest.newBuilder().setFriendlyName("cli-attack").build())
                .getId()));

              rpcClient.instanceStubBlocking().updateInstanceConfig(InstanceUpdateConfigRequest.newBuilder()
                .setId(cliInstanceId.toString())
                .setConfig(clientSettingsManager.exportSettingsProto(cliInstanceId))
                .build());

              rpcClient.instanceStubBlocking().changeInstanceState(InstanceStateChangeRequest.newBuilder()
                .setId(cliInstanceId.toString())
                .setState(InstanceState.RUNNING)
                .build());

              log.info("Attack started");

              return Command.SINGLE_SUCCESS;
            })));
  }

  @Override
  public int execute(String command, CommandSource source) {
    try {
      if (isClientCommand(command)) {
        log.debug("Executing command {} on client", command);
        return dispatcher.execute(command, new ClientConsoleCommandSource());
      } else {
        log.debug("Executing command {} on server", command);
        return rpcClient
          .commandStubBlocking()
          .executeCommand(CommandRequest.newBuilder().setCommand(command).build())
          .getCode();
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

  @Override
  public List<String> getCompletionSuggestions(String command, CommandSource source) {
    try {
      return rpcClient
        .commandStubBlocking()
        .tabCompleteCommand(CommandCompletionRequest.newBuilder().setCommand(command).build())
        .getSuggestionsList();
    } catch (Exception e) {
      log.error("An error occurred while trying to perform tab completion.", e);
      return List.of();
    }
  }
}
