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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/// Encoding node that compresses or decompresses data using GZIP.
public final class CompressionNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("encoding.compress")
    .displayName("Compress/Decompress")
    .category(CategoryRegistry.ENCODING)
    .addInputs(
      PortDefinition.input("input", "Input", PortType.STRING, "Data to compress/decompress"),
      PortDefinition.inputWithDefault("decompress", "Decompress", PortType.BOOLEAN, "false", "Decompress instead of compress")
    )
    .addOutputs(
      PortDefinition.output("output", "Output", PortType.STRING, "Compressed (base64) or decompressed data"),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether operation succeeded"),
      PortDefinition.output("ratio", "Ratio", PortType.NUMBER, "Compression ratio (original/compressed)")
    )
    .description("Compresses or decompresses data using GZIP")
    .icon("archive")
    .color("#64748B")
    .addKeywords("compress", "gzip", "decompress", "zip", "deflate")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var input = getStringInput(inputs, "input", "");
    var decompress = getBooleanInput(inputs, "decompress", false);

    try {
      if (decompress) {
        var compressed = Base64.getDecoder().decode(input);
        var bais = new ByteArrayInputStream(compressed);
        var gzis = new GZIPInputStream(bais);
        var decompressed = new String(gzis.readAllBytes(), StandardCharsets.UTF_8);
        gzis.close();

        return completedMono(results(
          "output", decompressed,
          "success", true,
          "ratio", (double) decompressed.length() / compressed.length
        ));
      } else {
        var original = input.getBytes(StandardCharsets.UTF_8);
        var baos = new ByteArrayOutputStream();
        var gzos = new GZIPOutputStream(baos);
        gzos.write(original);
        gzos.close();
        var compressed = baos.toByteArray();
        var ratio = compressed.length > 0 ? (double) original.length / compressed.length : 1.0;

        return completedMono(results(
          "output", Base64.getEncoder().encodeToString(compressed),
          "success", true,
          "ratio", ratio
        ));
      }
    } catch (Exception _) {
      return completedMono(results(
        "output", "",
        "success", false,
        "ratio", 1.0
      ));
    }
  }
}
