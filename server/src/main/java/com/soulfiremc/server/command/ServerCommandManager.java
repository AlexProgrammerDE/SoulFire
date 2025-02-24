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
package com.soulfiremc.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.soulfiremc.console.GenericTerminalConsole;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.adventure.SoulFireAdventure;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.lifecycle.CommandManagerInitEvent;
import com.soulfiremc.server.command.brigadier.BrigadierHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

/**
 * Holds and configures all server-side text commands of SoulFire itself.
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServerCommandManager {
  @Getter
  private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
  private final SoulFireServer soulFireServer;

  @PostConstruct
  public void postConstruct() {
    // Help
    HelpCommand.register(dispatcher);

    // Administration
    GenerateTokenCommand.register(dispatcher);
    SetEmailCommand.register(dispatcher);
    WhoAmICommand.register(dispatcher);
    PluginsCommand.register(dispatcher);

    // Pathfinding
    WalkCommand.register(dispatcher);
    CollectCommand.register(dispatcher);
    FollowCommand.register(dispatcher);
    ExcavateCommand.register(dispatcher);

    // Movement controls
    LookAtCommand.register(dispatcher);
    MoveCommand.register(dispatcher);
    JumpCommand.register(dispatcher);
    SneakCommand.register(dispatcher);
    ResetCommand.register(dispatcher);
    MimicCommand.register(dispatcher);

    // Inventory controls
    PlaceOnCommand.register(dispatcher);

    // Attack controls
    StartAttackCommand.register(dispatcher);
    PauseAttackCommand.register(dispatcher);
    StopAttackCommand.register(dispatcher);

    // Spark
    SparkCommand.register(dispatcher);

    // Utility commands
    OnlineCommand.register(dispatcher);
    SayCommand.register(dispatcher);
    StatsCommand.register(dispatcher);
    MetadataCommand.register(dispatcher);
    ExportMapCommand.register(dispatcher);
    StopTaskCommand.register(dispatcher);

    // Documentation commands
    PrintVersionsCommand.register(dispatcher);
    PrintCommandsCommand.register(dispatcher);
    PrintPluginsCommand.register(dispatcher);

    // Context commands
    BotCommand.register(dispatcher);
    InstanceCommand.register(dispatcher);
    RepeatCommand.register(dispatcher);

    SoulFireAPI.postEvent(new CommandManagerInitEvent(soulFireServer, this));
  }

  public int execute(String command, CommandSourceStack source) {
    command = command.strip();

    try {
      return dispatcher.execute(command, source);
    } catch (CommandSyntaxException e) {
      source.source().sendWarn(e.getMessage());
      return 0;
    }
  }

  public List<GenericTerminalConsole.Completion> complete(String command, int cursor, CommandSourceStack source) {
    return dispatcher
      .getCompletionSuggestions(dispatcher.parse(command, source), cursor)
      .join()
      .getList()
      .stream()
      .map(suggestion -> new GenericTerminalConsole.Completion(suggestion.getText(),
        SoulFireAdventure.TRUE_COLOR_ANSI_SERIALIZER.serializeOrNull(BrigadierHelper.toComponent(suggestion.getTooltip()))))
      .toList();
  }
}
