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

import io.grpc.Metadata;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class RPCConstants {
  public static final int LOGIN_RATE_LIMIT = 20;
  public static final String BEARER_TYPE = "Bearer";

  public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
    Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);

  private RPCConstants() {}
}
