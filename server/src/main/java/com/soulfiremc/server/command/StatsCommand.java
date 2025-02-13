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
import org.apache.commons.io.FileUtils;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public class StatsCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("stats")
        .executes(
          help(
            "Shows network stats",
            c ->
              forEveryInstanceEnsureHasBots(
                c,
                instanceManager -> {
                  var bots = getVisibleBots(instanceManager, c);
                  c.getSource().source()
                    .sendInfo(
                      "Total bots: {}", bots.size());
                  long readTraffic = 0;
                  long writeTraffic = 0;
                  for (var bot : bots) {
                    var trafficShapingHandler = bot.trafficHandler();

                    if (trafficShapingHandler == null) {
                      continue;
                    }

                    readTraffic +=
                      trafficShapingHandler.trafficCounter().cumulativeReadBytes();
                    writeTraffic +=
                      trafficShapingHandler.trafficCounter().cumulativeWrittenBytes();
                  }

                  c.getSource().source()
                    .sendInfo(
                      "Total read traffic: {}",
                      FileUtils.byteCountToDisplaySize(readTraffic));
                  c.getSource().source()
                    .sendInfo(
                      "Total write traffic: {}",
                      FileUtils.byteCountToDisplaySize(writeTraffic));

                  long currentReadTraffic = 0;
                  long currentWriteTraffic = 0;
                  for (var bot : bots) {
                    var trafficShapingHandler = bot.trafficHandler();

                    if (trafficShapingHandler == null) {
                      continue;
                    }

                    currentReadTraffic +=
                      trafficShapingHandler.trafficCounter().lastReadThroughput();
                    currentWriteTraffic +=
                      trafficShapingHandler.trafficCounter().lastWriteThroughput();
                  }

                  c.getSource().source()
                    .sendInfo(
                      "Current read traffic: {}/s",
                      FileUtils.byteCountToDisplaySize(currentReadTraffic));
                  c.getSource().source()
                    .sendInfo(
                      "Current write traffic: {}/s",
                      FileUtils.byteCountToDisplaySize(currentWriteTraffic));

                  return Command.SINGLE_SUCCESS;
                }))));
  }
}
