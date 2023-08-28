/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.grpc;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Executor;

@RequiredArgsConstructor
public class JwtCredential extends CallCredentials {
    private final String jwt;

    @Override
    public void applyRequestMetadata(final RequestInfo requestInfo, final Executor executor,
                                     final MetadataApplier metadataApplier) {
        executor.execute(() -> {
            try {
                Metadata headers = new Metadata();
                headers.put(Constant.AUTHORIZATION_METADATA_KEY,
                        String.format("%s %s", Constant.BEARER_TYPE, jwt));
                metadataApplier.apply(headers);
            } catch (Throwable e) {
                metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e));
            }
        });
    }
}
