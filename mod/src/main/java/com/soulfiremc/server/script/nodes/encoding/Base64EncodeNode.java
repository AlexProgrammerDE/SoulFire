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
package com.soulfiremc.server.script.nodes.encoding;

import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/// Encoding node that encodes a string to Base64.
public final class Base64EncodeNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("encoding.base64_encode")
    .displayName("Base64 Encode")
    .category(CategoryRegistry.ENCODING)
    .addInputs(
      PortDefinition.input("input", "Input", PortType.STRING, "String to encode"),
      PortDefinition.inputWithDefault("urlSafe", "URL Safe", PortType.BOOLEAN, "false", "Use URL-safe alphabet")
    )
    .addOutputs(
      PortDefinition.output("encoded", "Encoded", PortType.STRING, "Base64 encoded string")
    )
    .description("Encodes a string to Base64")
    .icon("file-code")
    .color("#64748B")
    .addKeywords("base64", "encode", "encoding")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var input = getStringInput(inputs, "input", "");
    var urlSafe = getBooleanInput(inputs, "urlSafe", false);

    var encoder = urlSafe ? Base64.getUrlEncoder() : Base64.getEncoder();
    var encoded = encoder.encodeToString(input.getBytes(StandardCharsets.UTF_8));

    return completedMono(result("encoded", encoded));
  }
}
