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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Encoding node that decodes a Base64 string.
public final class Base64DecodeNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("encoding.base64_decode")
    .displayName("Base64 Decode")
    .category(CategoryRegistry.ENCODING)
    .addInputs(
      PortDefinition.input("input", "Input", PortType.STRING, "Base64 string to decode"),
      PortDefinition.inputWithDefault("urlSafe", "URL Safe", PortType.BOOLEAN, "false", "Input uses URL-safe alphabet")
    )
    .addOutputs(
      PortDefinition.output("decoded", "Decoded", PortType.STRING, "Decoded string"),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether decoding succeeded")
    )
    .description("Decodes a Base64 string")
    .icon("file-code")
    .color("#64748B")
    .addKeywords("base64", "decode", "decoding")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var input = getStringInput(inputs, "input", "");
    var urlSafe = getBooleanInput(inputs, "urlSafe", false);

    try {
      var decoder = urlSafe ? Base64.getUrlDecoder() : Base64.getDecoder();
      var decoded = new String(decoder.decode(input), StandardCharsets.UTF_8);
      return completed(results(
        "decoded", decoded,
        "success", true
      ));
    } catch (IllegalArgumentException _) {
      return completed(results(
        "decoded", "",
        "success", false
      ));
    }
  }
}
