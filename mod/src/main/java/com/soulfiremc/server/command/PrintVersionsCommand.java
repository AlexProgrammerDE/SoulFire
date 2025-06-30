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
import com.viaversion.viaaprilfools.api.AprilFoolsProtocolVersion;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.vialoader.util.ProtocolVersionList;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.literal;
import static com.soulfiremc.server.command.brigadier.BrigadierHelper.privateCommand;

public final class PrintVersionsCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("print-versions")
        .requires(CommandSourceStack.IS_ADMIN)
        .executes(
          privateCommand(
            c -> {
              var builder = new StringBuilder("\n");
              ProtocolVersionList.getProtocolsNewToOld()
                .forEach(
                  version -> {
                    var versionId = "%s\\|%d".formatted(version.getVersionType().name(), version.getOriginalVersion());
                    String type;
                    if (BedrockProtocolVersion.PROTOCOLS.contains(version)) {
                      type = "BEDROCK";
                    } else if (LegacyProtocolVersion.PROTOCOLS.contains(version)) {
                      type = "LEGACY";
                    } else if (AprilFoolsProtocolVersion.PROTOCOLS.contains(version)) {
                      type = "SNAPSHOT";
                    } else {
                      type = "JAVA";
                    }

                    builder.append(
                      "| `%s`%s | `%s` | `%s` |\n".formatted(version.getName(), ProtocolTranslator.NATIVE_VERSION == version
                        ? " (native)" : "", versionId, type));
                  });
              c.getSource().source().sendInfo(builder.toString());

              return Command.SINGLE_SUCCESS;
            })));
  }
}
