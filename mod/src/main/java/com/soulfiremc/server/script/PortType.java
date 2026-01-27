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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The data type of a node port.
/// Used for connection validation and UI rendering.
/// Includes Blender-style type compatibility for implicit conversions.
public enum PortType {
  /// Any type - accepts all values.
  ANY,
  /// Numeric value (integer or floating point).
  NUMBER,
  /// Text string value.
  STRING,
  /// Boolean true/false value.
  BOOLEAN,
  /// 3D vector with x, y, z components.
  VECTOR3,
  /// Reference to a bot connection.
  BOT,
  /// List/array of values.
  LIST,
  /// Execution flow port (not data).
  EXEC,
  /// Block type identifier.
  BLOCK,
  /// Entity reference.
  ENTITY,
  /// Item stack reference.
  ITEM;

  /// Type compatibility matrix (Blender-style implicit conversions).
  /// Maps target type to set of source types that can be implicitly converted.
  private static final Map<PortType, Set<PortType>> IMPLICIT_CONVERSIONS = Map.ofEntries(
    // NUMBER can receive: NUMBER, BOOLEAN (false=0, true=1), STRING (parsed)
    Map.entry(NUMBER, Set.of(NUMBER, BOOLEAN, STRING)),
    // STRING can receive: most types via toString
    Map.entry(STRING, Set.of(NUMBER, STRING, BOOLEAN, VECTOR3, BLOCK, ITEM)),
    // BOOLEAN can receive: BOOLEAN, NUMBER (0=false, else=true), STRING ("true"/"false")
    Map.entry(BOOLEAN, Set.of(BOOLEAN, NUMBER, STRING)),
    // VECTOR3 can receive: VECTOR3, NUMBER (expanded to x,y,z), LIST (if 3 numbers)
    Map.entry(VECTOR3, Set.of(VECTOR3, NUMBER, LIST)),
    // LIST can receive: LIST, any single value (wrapped as single-element list)
    Map.entry(LIST, Set.of(LIST, ANY, NUMBER, STRING, BOOLEAN, VECTOR3, BOT, BLOCK, ENTITY, ITEM)),
    // BOT only accepts BOT
    Map.entry(BOT, Set.of(BOT)),
    // ENTITY only accepts ENTITY
    Map.entry(ENTITY, Set.of(ENTITY)),
    // ITEM only accepts ITEM
    Map.entry(ITEM, Set.of(ITEM)),
    // BLOCK only accepts BLOCK
    Map.entry(BLOCK, Set.of(BLOCK)),
    // EXEC only accepts EXEC
    Map.entry(EXEC, Set.of(EXEC)),
    // ANY accepts everything
    Map.entry(ANY, Set.of(values()))
  );

  /// Checks if this port type can accept a value from the given source type.
  /// Implements Blender-style implicit type conversion rules.
  ///
  /// @param sourceType the type of the source port
  /// @return true if the connection is valid (with or without conversion)
  public boolean canAccept(PortType sourceType) {
    // ANY accepts everything, anything can connect to ANY
    if (this == ANY || sourceType == ANY) {
      return true;
    }
    // Same type always works
    if (this == sourceType) {
      return true;
    }
    // Check the compatibility matrix
    var acceptable = IMPLICIT_CONVERSIONS.get(this);
    return acceptable != null && acceptable.contains(sourceType);
  }

  /// Gets a human-readable hint about how the conversion works.
  /// Used for UI tooltips when showing type conversion information.
  ///
  /// @param from source type
  /// @param to target type
  /// @return conversion hint string, or empty if same type or no conversion
  public static String getConversionHint(PortType from, PortType to) {
    if (from == to || to == ANY || from == ANY) {
      return "";
    }
    return switch (to) {
      case NUMBER -> switch (from) {
        case BOOLEAN -> "false→0, true→1";
        case STRING -> "Parsed as number, NaN on failure";
        case VECTOR3 -> "Average of x,y,z components";
        default -> "";
      };
      case STRING -> "Converted via toString()";
      case BOOLEAN -> switch (from) {
        case NUMBER -> "0→false, else→true";
        case STRING -> "\"true\"→true, else→false";
        default -> "";
      };
      case VECTOR3 -> switch (from) {
        case NUMBER -> "Expanded to (n, n, n)";
        case LIST -> "First 3 elements as x,y,z";
        default -> "";
      };
      case LIST -> "Wrapped as single-element list";
      default -> "";
    };
  }

  /// Gets all valid implicit conversions as a list of (from, to, hint) records.
  /// Used for populating the GetNodeTypesResponse with conversion rules.
  ///
  /// @return list of all valid type conversions with hints
  public static List<TypeConversionRecord> getAllConversions() {
    var conversions = new ArrayList<TypeConversionRecord>();
    for (var to : values()) {
      if (to == EXEC) continue; // EXEC is not a data type
      var acceptable = IMPLICIT_CONVERSIONS.get(to);
      if (acceptable == null) continue;
      for (var from : acceptable) {
        if (from == to || from == EXEC) continue; // Skip same type and EXEC
        var hint = getConversionHint(from, to);
        if (!hint.isEmpty()) {
          conversions.add(new TypeConversionRecord(from, to, hint));
        }
      }
    }
    return conversions;
  }

  /// Record representing a type conversion rule.
  public record TypeConversionRecord(PortType from, PortType to, String hint) {}
}
