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
package com.soulfiremc.server.protocol.bot.state.entity;

import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.data.NamedEntityData;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.mcstructs.AABB;
import com.soulfiremc.server.util.mcstructs.Direction;
import com.soulfiremc.server.util.mcstructs.MoverType;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;

public class Shulker extends Mob {
  private float currentPeekAmountO;
  private float currentPeekAmount;
  private int clientSideTeleportInterpolation;

  public Shulker(EntityType entityType, Level level) {
    super(entityType, level);
  }

  @Override
  public boolean canBeCollidedWith() {
    return this.isAlive();
  }

  @Override
  public void push(Entity entity) {
  }

  @Override
  public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    this.lerpSteps = 0;
    this.setPos(x, y, z);
    this.setRot(yRot, xRot);
  }

  private static float getPhysicalPeek(float peek) {
    return 0.5F - MathHelper.sin((0.5F + peek) * (float) Math.PI) * 0.5F;
  }

  public static AABB getProgressAabb(float scale, Direction expansionDirection, float peek, Vector3d position) {
    return getProgressDeltaAabb(scale, expansionDirection, -1.0F, peek, position);
  }

  public static AABB getProgressDeltaAabb(float scale, Direction expansionDirection, float currentPeek, float oldPeek, Vector3d position) {
    var bb = new AABB((double) (-scale) * 0.5, 0.0, (double) (-scale) * 0.5, (double) scale * 0.5, scale, (double) scale * 0.5);
    var maxPeek = (double) Math.max(currentPeek, oldPeek);
    var minPeek = (double) Math.min(currentPeek, oldPeek);
    var newBB = bb.expandTowards(
        (double) expansionDirection.getStepX() * maxPeek * (double) scale,
        (double) expansionDirection.getStepY() * maxPeek * (double) scale,
        (double) expansionDirection.getStepZ() * maxPeek * (double) scale
      )
      .contract(
        (double) (-expansionDirection.getStepX()) * (1.0 + minPeek) * (double) scale,
        (double) (-expansionDirection.getStepY()) * (1.0 + minPeek) * (double) scale,
        (double) (-expansionDirection.getStepZ()) * (1.0 + minPeek) * (double) scale
      );
    return newBB.move(position.getX(), position.getY(), position.getZ());
  }

  @Override
  public void tick() {
    super.tick();

    if (this.updatePeekAmount()) {
      this.onPeekAmountChange();
    }

    if (this.clientSideTeleportInterpolation > 0) {
      this.clientSideTeleportInterpolation--;
    }
  }

  private void onPeekAmountChange() {
    this.reapplyPosition();
    var newPeek = getPhysicalPeek(this.currentPeekAmount);
    var oldPeek = getPhysicalPeek(this.currentPeekAmountO);
    var oppositeFace = this.getAttachFace().getOpposite();
    var peekDiff = (newPeek - oldPeek) * this.getScale();
    if (!(peekDiff <= 0.0F)) {
      for (var entity : this.level()
        .getEntities(
          getProgressDeltaAabb(this.getScale(), oppositeFace, oldPeek, newPeek, this.pos())
        )) {
        if (!(entity instanceof Shulker) && !entity.noPhysics) {
          entity.move(MoverType.SHULKER, Vector3d.from(peekDiff * (float) oppositeFace.getStepX(), peekDiff * (float) oppositeFace.getStepY(), (double) (peekDiff * (float) oppositeFace.getStepZ())));
        }
      }
    }
  }

  @Override
  public void onSyncedDataUpdated(NamedEntityData entityData) {
    if (NamedEntityData.SHULKER__ATTACH_FACE.equals(entityData)) {
      this.setBoundingBox(this.makeBoundingBox());
    }

    super.onSyncedDataUpdated(entityData);
  }

  @Override
  protected AABB makeBoundingBox(Vector3d position) {
    var peek = getPhysicalPeek(this.currentPeekAmount);
    var oppositeFace = this.getAttachFace().getOpposite();
    return getProgressAabb(this.getScale(), oppositeFace, peek, position);
  }

  public Direction getAttachFace() {
    return Direction.fromMCPLDirection(this.metadataState.get(NamedEntityData.SHULKER__ATTACH_FACE, MetadataTypes.DIRECTION));
  }

  private boolean updatePeekAmount() {
    this.currentPeekAmountO = this.currentPeekAmount;
    var f = (float) this.getRawPeekAmount() * 0.01F;
    if (this.currentPeekAmount == f) {
      return false;
    } else {
      if (this.currentPeekAmount > f) {
        this.currentPeekAmount = MathHelper.clamp(this.currentPeekAmount - 0.05F, f, 1.0F);
      } else {
        this.currentPeekAmount = MathHelper.clamp(this.currentPeekAmount + 0.05F, 0.0F, f);
      }

      return true;
    }
  }

  private int getRawPeekAmount() {
    return this.metadataState.get(NamedEntityData.SHULKER__PEEK, MetadataTypes.INT);
  }
}
