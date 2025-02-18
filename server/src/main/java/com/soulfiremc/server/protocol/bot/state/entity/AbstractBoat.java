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
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;

public class AbstractBoat extends VehicleEntity {
  private final float[] paddlePositions = new float[2];
  private float invFriction;
  private float outOfControlTicks;
  private int lerpSteps;
  private double lerpX;
  private double lerpY;
  private double lerpZ;
  private double lerpYRot;
  private double lerpXRot;
  private AbstractBoat.Status status;
  private AbstractBoat.Status oldStatus;
  private double lastYd;
  private boolean isAboveBubbleColumn;
  private boolean bubbleColumnDirectionIsDown;
  private float bubbleMultiplier;
  private float bubbleAngle;
  private float bubbleAngleO;

  public AbstractBoat(EntityType entityType, Level level) {
    super(entityType, level);
  }

  @Override
  public boolean canCollideWith(Entity entity) {
    return entity.canBeCollidedWith() || entity.isPushable();
  }

  @Override
  public boolean canBeCollidedWith() {
    return true;
  }

  @Override
  public boolean isPushable() {
    return true;
  }

  @Override
  public void push(Entity entity) {
    if (entity instanceof AbstractBoat) {
      if (entity.getBoundingBox().minY < this.getBoundingBox().maxY) {
        super.push(entity);
      }
    } else if (entity.getBoundingBox().minY <= this.getBoundingBox().minY) {
      super.push(entity);
    }
  }

  private void tickBubbleColumn() {
    var i = this.getBubbleTime();
    if (i > 0) {
      this.bubbleMultiplier += 0.05F;
    } else {
      this.bubbleMultiplier -= 0.1F;
    }

    this.bubbleMultiplier = MathHelper.clamp(this.bubbleMultiplier, 0.0F, 1.0F);
    this.bubbleAngleO = this.bubbleAngle;
    this.bubbleAngle = 10.0F * (float) Math.sin((0.5F * (float) this.level().levelData().gameTime())) * this.bubbleMultiplier;
  }

  @Override
  public void cancelLerp() {
    this.lerpSteps = 0;
  }

  @Override
  public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    this.lerpX = x;
    this.lerpY = y;
    this.lerpZ = z;
    this.lerpYRot = yRot;
    this.lerpXRot = xRot;
    this.lerpSteps = steps;
  }

  @Override
  protected double getDefaultGravity() {
    return 0.04;
  }

  private void tickLerp() {
    if (this.lerpSteps > 0) {
      this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
      this.lerpSteps--;
    }
  }

  public boolean getPaddleState(int i) {
    return this.metadataState.getMetadata(i == 0 ? NamedEntityData.ABSTRACT_BOAT__ID_PADDLE_LEFT : NamedEntityData.ABSTRACT_BOAT__ID_PADDLE_RIGHT, MetadataTypes.BOOLEAN) && this.getControllingPassenger() != null;
  }

  private int getBubbleTime() {
    return this.metadataState.getMetadata(NamedEntityData.ABSTRACT_BOAT__ID_BUBBLE_TIME, MetadataTypes.INT);
  }

  private void setBubbleTime(int i) {
    this.metadataState.setMetadata(NamedEntityData.ABSTRACT_BOAT__ID_BUBBLE_TIME, MetadataTypes.INT, IntEntityMetadata::new, i);
  }

  public float getBubbleAngle(float f) {
    return MathHelper.lerp(f, this.bubbleAngleO, this.bubbleAngle);
  }

  public enum Status {
    IN_WATER,
    UNDER_WATER,
    UNDER_FLOWING_WATER,
    ON_LAND,
    IN_AIR
  }
}
