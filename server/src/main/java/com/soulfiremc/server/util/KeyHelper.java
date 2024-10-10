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
package com.soulfiremc.server.util;

import com.google.gson.JsonParseException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyHelper {
  private KeyHelper() {}

  public static String encodeBase64Key(Key key) {
    return Base64.getEncoder().encodeToString(key.getEncoded());
  }

  public static ECPublicKey decodeBase64PublicKey(String key) {
    try {
      var keyFactory = KeyFactory.getInstance("EC");
      return (ECPublicKey)
        keyFactory.generatePublic(new X509EncodedKeySpec(decodeBase64String(key)));
    } catch (GeneralSecurityException e) {
      throw new JsonParseException(e);
    }
  }

  public static ECPrivateKey decodeBase64PrivateKey(String key) {
    try {
      var keyFactory = KeyFactory.getInstance("EC");
      return (ECPrivateKey)
        keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodeBase64String(key)));
    } catch (GeneralSecurityException e) {
      throw new JsonParseException(e);
    }
  }

  private static byte[] decodeBase64String(String key) {
    return Base64.getDecoder().decode(key);
  }

  public static SecretKey getOrCreateJWTSecretKey(Path path) {
    try {
      if (!Files.exists(path)) {
        Files.write(path, KeyGenerator.getInstance("HmacSHA256").generateKey().getEncoded());
      }

      return new SecretKeySpec(Files.readAllBytes(path), "HmacSHA256");
    } catch (IOException | GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }
}
