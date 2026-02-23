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
package com.soulfiremc.server.script;

import java.util.*;

/// Single source of truth for port type compatibility rules.
/// Used by both the server-side graph validator and the client-side connection check
/// (via the PortTypeMetadata proto sent in GetNodeTypesResponse).
public final class TypeCompatibility {

  /// The complete compatibility table: for each target type, which source types can connect to it.
  /// This is the authoritative definition. Both isCompatible() and buildCompatibleFromMap()
  /// derive from the same logic.
  private static final Map<PortType, Set<PortType>> COMPATIBLE_FROM = buildCompatibleFromMap();

  private static Map<PortType, Set<PortType>> buildCompatibleFromMap() {
    var map = new EnumMap<PortType, Set<PortType>>(PortType.class);
    for (var type : PortType.values()) {
      map.put(type, EnumSet.noneOf(PortType.class));
    }

    // STRING accepts everything (coercion)
    map.get(PortType.STRING).addAll(EnumSet.of(PortType.NUMBER, PortType.BOOLEAN));

    // NUMBER accepts BOOLEAN and STRING
    map.get(PortType.NUMBER).addAll(EnumSet.of(PortType.STRING, PortType.BOOLEAN));

    // BOOLEAN accepts NUMBER and STRING
    map.get(PortType.BOOLEAN).addAll(EnumSet.of(PortType.NUMBER, PortType.STRING));

    // ANY accepts everything
    for (var type : PortType.values()) {
      if (type != PortType.EXEC && type != PortType.ANY) {
        map.get(PortType.ANY).add(type);
      }
    }

    return Collections.unmodifiableMap(map);
  }

  /// Checks whether a source port type is compatible with a target port type.
  /// Same type always matches. ANY on either side matches. STRING target accepts all.
  /// NUMBER<->BOOLEAN coercion is allowed.
  public static boolean isCompatible(PortType source, PortType target) {
    if (source == target) {
      return true;
    }
    if (target == PortType.ANY || source == PortType.ANY) {
      return true;
    }
    if (target == PortType.STRING) {
      return true; // anything can be coerced to string
    }
    var compatibleSet = COMPATIBLE_FROM.get(target);
    return compatibleSet != null && compatibleSet.contains(source);
  }

  /// Returns the compatible_from set for a given target type.
  /// Used to populate the PortTypeMetadata proto.
  public static Set<PortType> getCompatibleFrom(PortType targetType) {
    return COMPATIBLE_FROM.getOrDefault(targetType, Set.of());
  }

  private TypeCompatibility() {}
}
