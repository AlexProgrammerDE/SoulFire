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
package com.soulfiremc.server.bot;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.ClipContext;

import java.util.Objects;

/// Dispatches targeted attacks through the held weapon's native attack path.
public final class BotAttackSupport {
  private BotAttackSupport() {
  }

  public static boolean isFacingTarget(BotConnection bot, Entity target) {
    var player = Objects.requireNonNull(bot.minecraft().player);
    // The head used by piercing raycasts catches up during the player tick.
    return bot.rotationControl().isFacing(target.getEyePosition())
      && Math.abs(Mth.wrapDegrees(player.getYHeadRot() - player.getYRot())) < 0.01F;
  }

  public static boolean canPierceTarget(LocalPlayer player, Entity target) {
    return ProjectileUtil.getHitEntitiesAlong(
      player,
      player.getAttackRangeWith(player.getMainHandItem()),
      entity -> PiercingWeapon.canHitEntity(player, entity),
      ClipContext.Block.COLLIDER
    ).right().map(hits -> hits.stream()
      .anyMatch(hit -> target.is(hit.getEntity()) || hit.getEntity().is(target)))
      .orElse(false);
  }

  /// Returns false until the weapon is charged and a piercing target is in its attack path.
  public static boolean tryAttack(BotConnection bot, Entity target, boolean sprinting) {
    var player = Objects.requireNonNull(bot.minecraft().player);
    var gameMode = Objects.requireNonNull(bot.minecraft().gameMode);
    var heldItem = player.getMainHandItem();
    var piercingWeapon = heldItem.get(DataComponents.PIERCING_WEAPON);
    if (piercingWeapon != null) {
      var aim = target.getEyePosition();
      bot.rotationControl().lookAt(aim);
      if (!isFacingTarget(bot, target)
        || player.cannotAttackWithItem(heldItem, 0)
        || !canPierceTarget(player, target)) {
        return false;
      }
    }

    var wasSprinting = player.isSprinting();
    player.setSprinting(sprinting);
    try {
      if (piercingWeapon != null) {
        // Stabs use the server's view direction rather than an entity id.
        player.connection.send(new ServerboundMovePlayerPacket.Rot(
          player.getYRot(), player.getXRot(), player.onGround(), player.horizontalCollision
        ));
        gameMode.piercingAttack(heldItem.getAttackAnimation(), piercingWeapon);
      } else {
        gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND, heldItem.getAttackAnimation(), false);
        player.connection.send(ServerboundPunchPacket.INSTANCE);
      }
    } finally {
      player.setSprinting(wasSprinting);
    }
    return true;
  }
}
