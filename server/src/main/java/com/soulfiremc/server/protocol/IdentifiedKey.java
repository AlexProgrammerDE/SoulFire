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
package com.soulfiremc.server.protocol;

import com.google.common.collect.ImmutableSet;
import com.soulfiremc.server.util.EncryptionUtils;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class IdentifiedKey {
  private final Revision revision;
  private final PublicKey publicKey;
  private final byte[] signature;
  @Getter
  private final Instant expiryTemporal;
  private @MonotonicNonNull Boolean isSignatureValid;
  private @MonotonicNonNull UUID holder;

  public IdentifiedKey(Revision revision, byte[] keyBits, long expiry, byte[] signature) {
    this(
      revision,
      EncryptionUtils.parseRsaPublicKey(keyBits),
      Instant.ofEpochMilli(expiry),
      signature);
  }

  /**
   * Creates an Identified key from data.
   */
  public IdentifiedKey(
    Revision revision, PublicKey publicKey, Instant expiryTemporal, byte[] signature) {
    this.revision = revision;
    this.publicKey = publicKey;
    this.expiryTemporal = expiryTemporal;
    this.signature = signature;
  }

  public PublicKey getSignedPublicKey() {
    return publicKey;
  }

  public PublicKey getSigner() {
    return EncryptionUtils.getYggdrasilSessionKey();
  }

  public byte[] getSignature() {
    return signature.clone();
  }

  public @Nullable UUID getSignatureHolder() {
    return holder;
  }

  public Revision getKeyRevision() {
    return revision;
  }

  /**
   * Sets the uuid for this key. Returns false if incorrect.
   */
  public boolean internalAddHolder(UUID holder) {
    if (holder == null) {
      return false;
    }
    if (this.holder == null) {
      var result = validateData(holder);
      if (result == null || !result) {
        return false;
      }
      isSignatureValid = true;
      this.holder = holder;
      return true;
    }
    return this.holder.equals(holder) && isSignatureValid();
  }

  public boolean isSignatureValid() {
    if (isSignatureValid == null) {
      isSignatureValid = validateData(holder);
    }
    return isSignatureValid != null && isSignatureValid;
  }

  private Boolean validateData(@Nullable UUID verify) {
    if (revision == Revision.GENERIC_V1) {
      var pemKey = EncryptionUtils.pemEncodeRsaKey(publicKey);
      var expires = expiryTemporal.toEpochMilli();
      var toVerify = (expires + pemKey).getBytes(StandardCharsets.US_ASCII);
      return EncryptionUtils.verifySignature(
        EncryptionUtils.SHA1_WITH_RSA,
        EncryptionUtils.getYggdrasilSessionKey(),
        signature,
        toVerify);
    } else {
      if (verify == null) {
        return null;
      }
      var keyBytes = publicKey.getEncoded();
      var toVerify = new byte[keyBytes.length + 24]; // length long * 3
      var fixedDataSet = ByteBuffer.wrap(toVerify).order(ByteOrder.BIG_ENDIAN);
      fixedDataSet.putLong(verify.getMostSignificantBits());
      fixedDataSet.putLong(verify.getLeastSignificantBits());
      fixedDataSet.putLong(expiryTemporal.toEpochMilli());
      fixedDataSet.put(keyBytes);
      return EncryptionUtils.verifySignature(
        EncryptionUtils.SHA1_WITH_RSA,
        EncryptionUtils.getYggdrasilSessionKey(),
        signature,
        toVerify);
    }
  }

  public boolean verifyDataSignature(byte[] signature, byte[]... toVerify) {
    try {
      return EncryptionUtils.verifySignature(
        EncryptionUtils.SHA256_WITH_RSA, publicKey, signature, toVerify);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Getter
  @RequiredArgsConstructor
  public enum Revision {
    GENERIC_V1(ImmutableSet.of(), ImmutableSet.of(ProtocolVersion.v1_19)),
    LINKED_V2(ImmutableSet.of(), ImmutableSet.of(ProtocolVersion.v1_19_1));

    final Set<Revision> backwardsCompatibleTo;
    final Set<ProtocolVersion> applicableTo;
  }
}
