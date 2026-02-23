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

import com.soulfiremc.server.script.PortType;
import com.soulfiremc.server.script.TypeCompatibility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for {@link TypeCompatibility} ensuring isCompatible() and getCompatibleFrom() are consistent.
final class TypeCompatibilityTest {

  @Test
  void sameTypeIsAlwaysCompatible() {
    for (var type : PortType.values()) {
      assertTrue(TypeCompatibility.isCompatible(type, type),
        type + " should be compatible with itself");
    }
  }

  @Test
  void anyAcceptsEverything() {
    for (var type : PortType.values()) {
      if (type == PortType.EXEC) continue;
      assertTrue(TypeCompatibility.isCompatible(type, PortType.ANY),
        type + " should be accepted by ANY target");
      assertTrue(TypeCompatibility.isCompatible(PortType.ANY, type),
        "ANY source should be accepted by " + type + " target");
    }
  }

  @Test
  void stringAcceptsEverything() {
    for (var type : PortType.values()) {
      if (type == PortType.EXEC) continue;
      assertTrue(TypeCompatibility.isCompatible(type, PortType.STRING),
        type + " should be accepted by STRING target");
    }
  }

  @Test
  void numberBooleanCoercion() {
    assertTrue(TypeCompatibility.isCompatible(PortType.NUMBER, PortType.BOOLEAN));
    assertTrue(TypeCompatibility.isCompatible(PortType.BOOLEAN, PortType.NUMBER));
  }

  @Test
  void botEntityBlockItemNotInterchangeable() {
    assertFalse(TypeCompatibility.isCompatible(PortType.BOT, PortType.ENTITY));
    assertFalse(TypeCompatibility.isCompatible(PortType.ENTITY, PortType.BOT));
    assertFalse(TypeCompatibility.isCompatible(PortType.BLOCK, PortType.ITEM));
    assertFalse(TypeCompatibility.isCompatible(PortType.NUMBER, PortType.BOT));
  }

  @Test
  void compatibleFromMatchesIsCompatible() {
    for (var target : PortType.values()) {
      if (target == PortType.EXEC) continue;
      var compatibleSet = TypeCompatibility.getCompatibleFrom(target);
      for (var source : PortType.values()) {
        if (source == PortType.EXEC || source == target) continue;
        // ANY always matches via isCompatible but may not be in compatibleFrom
        if (source == PortType.ANY || target == PortType.ANY || target == PortType.STRING) continue;
        var inSet = compatibleSet.contains(source);
        var isCompat = TypeCompatibility.isCompatible(source, target);
        assertEquals(inSet, isCompat,
          source + " -> " + target + ": compatibleFrom says " + inSet + " but isCompatible says " + isCompat);
      }
    }
  }
}
