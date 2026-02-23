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
package com.soulfiremc.test.script;

import com.soulfiremc.server.script.NodeValue;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.soulfiremc.test.script.ScriptTestHelper.executeNode;
import static org.junit.jupiter.api.Assertions.*;

/// Tests for encoding nodes: Base64Encode, Base64Decode, Hash.
final class EncodingNodeTest {

  // --- Base64 Encode ---

  @Test
  void base64EncodeBasicString() {
    var outputs = executeNode("encoding.base64_encode", Map.of(
      "input", NodeValue.ofString("hello")
    ));
    assertEquals("aGVsbG8=", outputs.get("encoded").asString(""));
  }

  @Test
  void base64EncodeEmptyString() {
    var outputs = executeNode("encoding.base64_encode", Map.of(
      "input", NodeValue.ofString("")
    ));
    assertEquals("", outputs.get("encoded").asString("X"));
  }

  @Test
  void base64EncodeUrlSafe() {
    // URL-safe base64 uses - and _ instead of + and /
    var outputs = executeNode("encoding.base64_encode", Map.of(
      "input", NodeValue.ofString("subjects?_d"),
      "urlSafe", NodeValue.ofBoolean(true)
    ));
    var encoded = outputs.get("encoded").asString("");
    assertFalse(encoded.contains("+"), "URL-safe should not contain +");
    assertFalse(encoded.contains("/"), "URL-safe should not contain /");
  }

  @Test
  void base64EncodeUnicode() {
    var outputs = executeNode("encoding.base64_encode", Map.of(
      "input", NodeValue.ofString("\u00e4\u00f6\u00fc")
    ));
    assertFalse(outputs.get("encoded").asString("").isEmpty(),
      "Unicode encoding should produce output");
  }

  // --- Base64 Decode ---

  @Test
  void base64DecodeBasicString() {
    var outputs = executeNode("encoding.base64_decode", Map.of(
      "input", NodeValue.ofString("aGVsbG8=")
    ));
    assertEquals("hello", outputs.get("decoded").asString(""));
    assertTrue(outputs.get("success").asBoolean(false));
  }

  @Test
  void base64DecodeInvalidInput() {
    var outputs = executeNode("encoding.base64_decode", Map.of(
      "input", NodeValue.ofString("!!!not-base64!!!")
    ));
    assertFalse(outputs.get("success").asBoolean(true),
      "Decoding invalid input should fail");
  }

  @Test
  void base64DecodeUrlSafe() {
    // Encode with URL-safe, then decode with URL-safe
    var encoded = executeNode("encoding.base64_encode", Map.of(
      "input", NodeValue.ofString("subjects?_d"),
      "urlSafe", NodeValue.ofBoolean(true)
    ));
    var decoded = executeNode("encoding.base64_decode", Map.of(
      "input", encoded.get("encoded"),
      "urlSafe", NodeValue.ofBoolean(true)
    ));
    assertEquals("subjects?_d", decoded.get("decoded").asString(""));
    assertTrue(decoded.get("success").asBoolean(false));
  }

  @Test
  void base64RoundTrip() {
    var original = "Hello, World! 123 \u00e4\u00f6\u00fc";
    var encoded = executeNode("encoding.base64_encode", Map.of(
      "input", NodeValue.ofString(original)
    ));
    var decoded = executeNode("encoding.base64_decode", Map.of(
      "input", encoded.get("encoded")
    ));
    assertEquals(original, decoded.get("decoded").asString(""));
  }

  // --- Hash ---

  @Test
  void hashSha256Hex() {
    var outputs = executeNode("encoding.hash", Map.of(
      "input", NodeValue.ofString("hello"),
      "algorithm", NodeValue.ofString("SHA-256"),
      "encoding", NodeValue.ofString("hex")
    ));
    assertTrue(outputs.get("success").asBoolean(false));
    assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
      outputs.get("hash").asString(""),
      "SHA-256 of 'hello' should match known hash");
  }

  @Test
  void hashSha256Base64() {
    var outputs = executeNode("encoding.hash", Map.of(
      "input", NodeValue.ofString("hello"),
      "algorithm", NodeValue.ofString("SHA-256"),
      "encoding", NodeValue.ofString("base64")
    ));
    assertTrue(outputs.get("success").asBoolean(false));
    assertFalse(outputs.get("hash").asString("").isEmpty());
  }

  @Test
  void hashMd5() {
    var outputs = executeNode("encoding.hash", Map.of(
      "input", NodeValue.ofString("hello"),
      "algorithm", NodeValue.ofString("MD5"),
      "encoding", NodeValue.ofString("hex")
    ));
    assertTrue(outputs.get("success").asBoolean(false));
    assertEquals("5d41402abc4b2a76b9719d911017c592",
      outputs.get("hash").asString(""),
      "MD5 of 'hello' should match known hash");
  }

  @Test
  void hashInvalidAlgorithm() {
    var outputs = executeNode("encoding.hash", Map.of(
      "input", NodeValue.ofString("hello"),
      "algorithm", NodeValue.ofString("NOT-A-REAL-ALGO"),
      "encoding", NodeValue.ofString("hex")
    ));
    assertFalse(outputs.get("success").asBoolean(true),
      "Invalid algorithm should fail");
  }

  @Test
  void hashEmptyString() {
    var outputs = executeNode("encoding.hash", Map.of(
      "input", NodeValue.ofString(""),
      "algorithm", NodeValue.ofString("SHA-256"),
      "encoding", NodeValue.ofString("hex")
    ));
    assertTrue(outputs.get("success").asBoolean(false));
    assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      outputs.get("hash").asString(""),
      "SHA-256 of empty string should match known hash");
  }

  // --- Compression ---

  @Test
  void compressDecompressRoundTrip() {
    var original = "Hello, World! This is a test of GZIP compression.";
    var compressed = executeNode("encoding.compress", Map.of(
      "input", NodeValue.ofString(original),
      "decompress", NodeValue.ofBoolean(false)
    ));
    assertTrue(compressed.get("success").asBoolean(false), "Compression should succeed");
    assertFalse(compressed.get("output").asString("").isEmpty(), "Compressed output should not be empty");

    var decompressed = executeNode("encoding.compress", Map.of(
      "input", compressed.get("output"),
      "decompress", NodeValue.ofBoolean(true)
    ));
    assertTrue(decompressed.get("success").asBoolean(false), "Decompression should succeed");
    assertEquals(original, decompressed.get("output").asString(""),
      "Decompressed data should match original");
  }

  @Test
  void compressInvalidDecompressInput() {
    var outputs = executeNode("encoding.compress", Map.of(
      "input", NodeValue.ofString("not-valid-gzip-base64"),
      "decompress", NodeValue.ofBoolean(true)
    ));
    assertFalse(outputs.get("success").asBoolean(true),
      "Decompressing invalid input should fail");
  }

  // --- Encryption ---

  @Test
  void encryptDecryptRoundTrip() {
    var original = "Secret message for testing";
    var key = "my-secret-key";

    var encrypted = executeNode("encoding.encrypt", Map.of(
      "plaintext", NodeValue.ofString(original),
      "key", NodeValue.ofString(key)
    ));
    assertTrue(encrypted.get("success").asBoolean(false), "Encryption should succeed");
    assertFalse(encrypted.get("ciphertext").asString("").isEmpty(),
      "Ciphertext should not be empty");

    var decrypted = executeNode("encoding.decrypt", Map.of(
      "ciphertext", encrypted.get("ciphertext"),
      "key", NodeValue.ofString(key)
    ));
    assertTrue(decrypted.get("success").asBoolean(false), "Decryption should succeed");
    assertEquals(original, decrypted.get("plaintext").asString(""),
      "Decrypted text should match original");
  }

  @Test
  void encryptDecryptWrongKeyFails() {
    var original = "Secret message";
    var encrypted = executeNode("encoding.encrypt", Map.of(
      "plaintext", NodeValue.ofString(original),
      "key", NodeValue.ofString("correct-key")
    ));
    assertTrue(encrypted.get("success").asBoolean(false));

    var decrypted = executeNode("encoding.decrypt", Map.of(
      "ciphertext", encrypted.get("ciphertext"),
      "key", NodeValue.ofString("wrong-key")
    ));
    assertFalse(decrypted.get("success").asBoolean(true),
      "Decryption with wrong key should fail");
  }

  @Test
  void encryptEmptyKeyFails() {
    var outputs = executeNode("encoding.encrypt", Map.of(
      "plaintext", NodeValue.ofString("test"),
      "key", NodeValue.ofString("")
    ));
    assertFalse(outputs.get("success").asBoolean(true),
      "Encryption with empty key should fail");
  }
}
