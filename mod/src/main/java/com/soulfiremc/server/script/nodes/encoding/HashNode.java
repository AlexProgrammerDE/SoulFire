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
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Encoding node that computes a cryptographic hash of the input.
public final class HashNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("encoding.hash")
    .displayName("Hash")
    .category(CategoryRegistry.ENCODING)
    .addInputs(
      PortDefinition.input("input", "Input", PortType.STRING, "String to hash"),
      PortDefinition.inputWithDefault("algorithm", "Algorithm", PortType.STRING, "\"SHA-256\"", "Hash algorithm (MD5, SHA-1, SHA-256, SHA-512)"),
      PortDefinition.inputWithDefault("encoding", "Encoding", PortType.STRING, "\"hex\"", "Output encoding (hex, base64)")
    )
    .addOutputs(
      PortDefinition.output("hash", "Hash", PortType.STRING, "Computed hash"),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether hashing succeeded")
    )
    .description("Computes a cryptographic hash of the input")
    .icon("hash")
    .color("#64748B")
    .addKeywords("hash", "md5", "sha", "sha256", "checksum", "digest")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var input = getStringInput(inputs, "input", "");
    var algorithm = getStringInput(inputs, "algorithm", "SHA-256");
    var encoding = getStringInput(inputs, "encoding", "hex");

    try {
      var digest = MessageDigest.getInstance(algorithm);
      var hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

      String result = switch (encoding.toLowerCase()) {
        case "base64" -> Base64.getEncoder().encodeToString(hashBytes);
        default -> HexFormat.of().formatHex(hashBytes);
      };

      return completed(results(
        "hash", result,
        "success", true
      ));
    } catch (Exception _) {
      return completed(results(
        "hash", "",
        "success", false
      ));
    }
  }
}
