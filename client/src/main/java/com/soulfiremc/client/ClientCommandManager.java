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

import com.soulfiremc.client.cli.CLIManager;
import com.soulfiremc.client.grpc.RPCClient;
import com.soulfiremc.console.GenericTerminalConsole;
import com.soulfiremc.grpc.generated.CommandCompletionRequest;
import com.soulfiremc.grpc.generated.CommandRequest;
import com.soulfiremc.grpc.generated.InstanceCommandScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ClientCommandManager {
  private final RPCClient rpcClient;
  private final CLIManager cliManager;

  public int execute(String command) {
    log.debug("Executing command {} on server", command);
    return rpcClient
      .commandStubBlocking()
      .executeCommand(CommandRequest.newBuilder()
        .setInstance(InstanceCommandScope.newBuilder()
          .setInstanceId(cliManager.cliInstanceId().toString())
          .build())
        .setCommand(command)
        .build())
      .getCode();
  }

  public List<GenericTerminalConsole.Completion> complete(String command, int cursor) {
    log.debug("Getting completion suggestions for command {} on server", command);
    return rpcClient
      .commandStubBlocking()
      .tabCompleteCommand(CommandCompletionRequest.newBuilder()
        .setInstance(InstanceCommandScope.newBuilder()
          .setInstanceId(cliManager.cliInstanceId().toString())
          .build())
        .setCommand(command)
        .setCursor(cursor)
        .build())
      .getSuggestionsList()
      .stream()
      .map(suggestion -> new GenericTerminalConsole.Completion(
        suggestion.getSuggestion(),
        suggestion.hasTooltip() ? suggestion.getTooltip() : null
      ))
      .toList();
  }
}
