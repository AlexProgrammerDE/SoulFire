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
package com.soulfiremc.server.script.nodes.util;

import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/// Utility node that gets the current timestamp in various formats.
public final class TimestampNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("utility.timestamp")
    .displayName("Timestamp")
    .category(CategoryRegistry.UTILITY)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("format", "Format", PortType.STRING, "\"\"", "Date format pattern (empty = epoch ms)"),
      PortDefinition.inputWithDefault("timezone", "Timezone", PortType.STRING, "\"UTC\"", "Timezone ID (e.g., America/New_York)")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("timestamp", "Timestamp", PortType.NUMBER, "Epoch milliseconds"),
      PortDefinition.output("formatted", "Formatted", PortType.STRING, "Formatted date string"),
      PortDefinition.output("iso", "ISO", PortType.STRING, "ISO 8601 formatted string")
    )
    .description("Gets the current timestamp in various formats")
    .icon("clock")
    .color("#6B7280")
    .addKeywords("time", "timestamp", "date", "now", "epoch", "clock")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var format = getStringInput(inputs, "format", "");
    var timezone = getStringInput(inputs, "timezone", "UTC");

    var now = Instant.now();
    ZoneId zone;
    try {
      zone = ZoneId.of(timezone);
    } catch (Exception _) {
      zone = ZoneId.of("UTC");
    }
    var zoned = ZonedDateTime.ofInstant(now, zone);

    String formatted;
    if (format.isEmpty()) {
      formatted = String.valueOf(now.toEpochMilli());
    } else {
      try {
        formatted = DateTimeFormatter.ofPattern(format).format(zoned);
      } catch (Exception _) {
        formatted = String.valueOf(now.toEpochMilli());
      }
    }

    return completedMono(results(
      "timestamp", now.toEpochMilli(),
      "formatted", formatted,
      "iso", zoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    ));
  }
}
