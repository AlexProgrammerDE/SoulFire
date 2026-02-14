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
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/// Encoding node that encrypts a string using AES encryption.
public final class EncryptNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("encoding.encrypt")
    .displayName("Encrypt")
    .category(CategoryRegistry.ENCODING)
    .addInputs(
      PortDefinition.input("plaintext", "Plaintext", PortType.STRING, "Text to encrypt"),
      PortDefinition.input("key", "Key", PortType.STRING, "Encryption key (will be hashed to 256 bits)")
    )
    .addOutputs(
      PortDefinition.output("ciphertext", "Ciphertext", PortType.STRING, "Base64-encoded encrypted data (includes IV)"),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether encryption succeeded"),
      PortDefinition.output("errorMessage", "Error Message", PortType.STRING, "Error message if failed")
    )
    .description("Encrypts a string using AES encryption")
    .icon("lock")
    .color("#64748B")
    .addKeywords("encrypt", "aes", "cipher", "secure", "protection")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var plaintext = getStringInput(inputs, "plaintext", "");
    var key = getStringInput(inputs, "key", "");

    if (key.isEmpty()) {
      return completedMono(results(
        "ciphertext", "",
        "success", false,
        "errorMessage", "Key is required"
      ));
    }

    try {
      // Derive 256-bit key from user key
      var keyHash = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
      var secretKey = new SecretKeySpec(keyHash, "AES");

      // Generate random IV
      var iv = new byte[16];
      new SecureRandom().nextBytes(iv);
      var ivSpec = new IvParameterSpec(iv);

      // Encrypt
      var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
      var encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      // Prepend IV to ciphertext and encode
      var combined = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

      return completedMono(results(
        "ciphertext", Base64.getEncoder().encodeToString(combined),
        "success", true,
        "errorMessage", ""
      ));
    } catch (Exception e) {
      return completedMono(results(
        "ciphertext", "",
        "success", false,
        "errorMessage", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
      ));
    }
  }
}
