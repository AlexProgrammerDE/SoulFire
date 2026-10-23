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
package com.soulfiremc.server.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class SFEntityHelpersTest {
  @Test
  void rejectsTargetsThatVanillaTreatsAsInvalidAttackPackets() {
    var attacker = mock(Entity.class);
    assertFalse(SFEntityHelpers.isValidAttackTarget(attacker, attacker));
    assertFalse(SFEntityHelpers.isValidAttackTarget(attacker, mock(ItemEntity.class)));
    assertFalse(SFEntityHelpers.isValidAttackTarget(attacker, mock(ExperienceOrb.class)));

    var arrow = mock(AbstractArrow.class);
    assertFalse(SFEntityHelpers.isValidAttackTarget(attacker, arrow));
    when(arrow.isAttackable()).thenReturn(true);
    assertTrue(SFEntityHelpers.isValidAttackTarget(attacker, arrow));

    assertTrue(SFEntityHelpers.isValidAttackTarget(attacker, mock(ArmorStand.class)));
    assertTrue(SFEntityHelpers.isValidAttackTarget(attacker, mock(Entity.class)));
  }

  @Test
  void excludesGenericAndDragonDeathAnimationsFromTargeting() {
    assertTrue(SFEntityHelpers.isAliveAndTargetable(true, false, false));
    assertFalse(SFEntityHelpers.isAliveAndTargetable(false, false, false));
    assertFalse(SFEntityHelpers.isAliveAndTargetable(true, true, false));
    assertFalse(SFEntityHelpers.isAliveAndTargetable(true, false, true));
  }
}
