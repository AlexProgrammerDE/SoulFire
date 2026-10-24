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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/// Entity lifecycle checks used by queries and tasks that need an actionable
/// target rather than an entity which is still rendering its death animation.
public final class SFEntityHelpers {
  private SFEntityHelpers() {
  }

  /// Mirrors vanilla's attack-packet validation to avoid disconnecting the bot.
  public static boolean isValidAttackTarget(Entity attacker, Entity target) {
    return target != attacker
      && !(target instanceof ItemEntity)
      && !(target instanceof ExperienceOrb)
      && (!(target instanceof AbstractArrow arrow) || arrow.isAttackable());
  }

  public static boolean isAliveAndTargetable(Entity entity) {
    var livingDeadOrDying = entity instanceof LivingEntity living
      && living.isDeadOrDying();
    var dragonDying = entity instanceof EnderDragon dragon
      && (dragon.dragonDeathTime > 0
      || dragon.getPhaseManager().getCurrentPhase().getPhase()
      == EnderDragonPhase.DYING);
    return isAliveAndTargetable(
      entity.isAlive(),
      livingDeadOrDying,
      dragonDying
    );
  }

  static boolean isAliveAndTargetable(
    boolean alive,
    boolean livingDeadOrDying,
    boolean dragonDying
  ) {
    return alive && !livingDeadOrDying && !dragonDying;
  }
}
