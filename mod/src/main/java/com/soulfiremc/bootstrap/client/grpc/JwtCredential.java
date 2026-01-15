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
package com.soulfiremc.bootstrap.client.grpc;

import com.soulfiremc.server.util.RPCConstants;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Executor;

@RequiredArgsConstructor
public final class JwtCredential extends CallCredentials {
  private final String jwt;

  @Override
  public void applyRequestMetadata(
    final RequestInfo requestInfo,
    final Executor executor,
    final MetadataApplier metadataApplier) {
    executor.execute(
      () -> {
        try {
          var headers = new Metadata();
          headers.put(
            RPCConstants.AUTHORIZATION_METADATA_KEY,
            "Bearer %s".formatted(jwt));
          metadataApplier.apply(headers);
        } catch (Throwable e) {
          metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e));
        }
      });
  }
}
