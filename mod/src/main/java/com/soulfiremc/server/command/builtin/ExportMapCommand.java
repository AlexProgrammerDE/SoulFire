/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.soulfiremc.server.command.CommandSourceStack;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.SFPathConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.saveddata.maps.MapId;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.function.Function;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public final class ExportMapCommand {
  private ExportMapCommand() {
  }

  private static int exportMap(
    CommandContext<CommandSourceStack> context, Function<ClientLevel, Set<MapId>> idProvider) throws CommandSyntaxException {
    // Inside here to capture a time for the file name
    var currentTime = System.currentTimeMillis();
    return forEveryBot(
      context,
      bot -> {
        var level = bot.minecraft().level;
        if (level == null) {
          context.getSource().source().sendWarn("No level loaded!");
          return Command.SINGLE_SUCCESS;
        }

        for (var mapId : idProvider.apply(level)) {
          var mapDataState = level.getMapData(mapId);
          if (mapDataState == null) {
            context.getSource().source().sendInfo("Map not found!");
            return Command.SINGLE_SUCCESS;
          }

          var image = SFHelpers.toBufferedImage(mapDataState);
          var fileName = "map_%d_%d_%s.png".formatted(currentTime, mapId.id(), bot.accountName());
          try {
            var mapsDirectory = SFPathConstants.getMapsDirectory(bot.instanceManager().getInstanceObjectStoragePath());
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

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("export-map")
        .executes(
          help(
            "Exports images of all map items. Can be a held item or in a item-frame.",
            c -> exportMap(c, level -> level.getAllMapData().keySet())))
        .then(
          argument("map_id", IntegerArgumentType.integer())
            .executes(
              help(
                "Exports a image of a map item by map id. Can be a held item or in a item-frame.",
                c -> {
                  var mapId = IntegerArgumentType.getInteger(c, "map_id");
                  return exportMap(c, _ -> Set.of(new MapId(mapId)));
                }))));
  }
}
