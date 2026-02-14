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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

/// Encoding node that decrypts an AES-encrypted string.
public final class DecryptNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("encoding.decrypt")
    .displayName("Decrypt")
    .category(CategoryRegistry.ENCODING)
    .addInputs(
      PortDefinition.input("ciphertext", "Ciphertext", PortType.STRING, "Base64-encoded encrypted data"),
      PortDefinition.input("key", "Key", PortType.STRING, "Decryption key")
    )
    .addOutputs(
      PortDefinition.output("plaintext", "Plaintext", PortType.STRING, "Decrypted text"),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether decryption succeeded"),
      PortDefinition.output("errorMessage", "Error Message", PortType.STRING, "Error message if failed")
    )
    .description("Decrypts an AES-encrypted string")
    .icon("unlock")
    .color("#64748B")
    .addKeywords("decrypt", "aes", "cipher", "decipher")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var ciphertext = getStringInput(inputs, "ciphertext", "");
    var key = getStringInput(inputs, "key", "");

    if (key.isEmpty()) {
      return completedMono(results(
        "plaintext", "",
        "success", false,
        "errorMessage", "Key is required"
      ));
    }

    try {
      var keyHash = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
      var secretKey = new SecretKeySpec(keyHash, "AES");

      var combined = Base64.getDecoder().decode(ciphertext);

      if (combined.length < 16) {
        return completedMono(results(
          "plaintext", "",
          "success", false,
          "errorMessage", "Invalid ciphertext"
        ));
      }

      // Extract IV (first 16 bytes)
      var iv = new byte[16];
      var encrypted = new byte[combined.length - 16];
      System.arraycopy(combined, 0, iv, 0, 16);
      System.arraycopy(combined, 16, encrypted, 0, encrypted.length);

      var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
      var decrypted = cipher.doFinal(encrypted);

      return completedMono(results(
        "plaintext", new String(decrypted, StandardCharsets.UTF_8),
        "success", true,
        "errorMessage", ""
      ));
    } catch (Exception e) {
      return completedMono(results(
        "plaintext", "",
        "success", false,
        "errorMessage", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
      ));
    }
  }
}
