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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.util.SFPathConstants;
import it.unimi.dsi.fastutil.ints.IntSet;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Function;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public class ExportMapCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("export-map")
        .executes(
          help(
            "Exports images of all map items. Can be a held item or in a item-frame.",
            c -> exportMap(c, bot -> bot.dataManager().mapDataStates().keySet())))
        .then(
          argument("map_id", IntegerArgumentType.integer())
            .executes(
              help(
                "Exports a image of a map item by map id. Can be a held item or in a item-frame.",
                c -> {
                  var mapId = IntegerArgumentType.getInteger(c, "map_id");
                  return exportMap(c, bot -> IntSet.of(mapId));
                }))));
  }

  private static int exportMap(
    CommandContext<CommandSourceStack> context, Function<BotConnection, IntSet> idProvider) throws CommandSyntaxException {
    // Inside here to capture a time for the file name
    var currentTime = System.currentTimeMillis();
    return forEveryBot(
      context,
      bot -> {
        for (var mapId : idProvider.apply(bot).toIntArray()) {
          var mapDataState = bot.dataManager().mapDataStates().get(mapId);
          if (mapDataState == null) {
            context.getSource().source().sendInfo("Map not found!");
            return Command.SINGLE_SUCCESS;
          }

          var image = mapDataState.toBufferedImage();
          var fileName = "map_%d_%d_%s.png".formatted(currentTime, mapId, bot.accountName());
          try {
            var mapsDirectory = SFPathConstants.getMapsDirectory(bot.instanceManager().getObjectStoragePath());
            Files.createDirectories(mapsDirectory);
            var file = mapsDirectory.resolve(fileName);
            ImageIO.write(image, "png", file.toFile());
            context.getSource().source().sendInfo("Exported map to {}", file);
          } catch (IOException e) {
            context.getSource().source().sendError("Failed to export map!", e);
          }
        }

        return Command.SINGLE_SUCCESS;
      });
  }
}
