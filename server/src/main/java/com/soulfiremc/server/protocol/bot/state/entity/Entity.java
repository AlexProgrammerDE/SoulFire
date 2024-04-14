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

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import com.soulfiremc.server.data.AttributeType;
import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.data.ResourceKey;
import com.soulfiremc.server.protocol.bot.movement.AABB;
import com.soulfiremc.server.protocol.bot.state.EntityAttributeState;
import com.soulfiremc.server.protocol.bot.state.EntityEffectState;
import com.soulfiremc.server.protocol.bot.state.EntityMetadataState;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

@Slf4j
@Getter
@Setter
public abstract class Entity {
  public static final float BREATHING_DISTANCE_BELOW_EYES = 0.11111111F;
  private final EntityMetadataState metadataState = new EntityMetadataState();
  private final EntityAttributeState attributeState = new EntityAttributeState();
  private final EntityEffectState effectState = new EntityEffectState();
  private final int entityId;
  private final UUID uuid;
  private final EntityType entityType;
  protected Level level;
  protected double x;
  protected double y;
  protected double z;
  protected float yaw;
  protected float pitch;
  protected float headYaw;
  protected double motionX;
  protected double motionY;
  protected double motionZ;
  protected boolean onGround;

  public Entity(int entityId, UUID uuid, EntityType entityType,
                Level level,
                double x, double y, double z,
                float yaw, float pitch, float headYaw,
                double motionX, double motionY, double motionZ) {
    this.entityId = entityId;
    this.uuid = uuid;
    this.entityType = entityType;
    this.level = level;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.headYaw = headYaw;
    this.motionX = motionX;
    this.motionY = motionY;
    this.motionZ = motionZ;
  }

  public void setPosition(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public void addPosition(double deltaX, double deltaY, double deltaZ) {
    this.x += deltaX;
    this.y += deltaY;
    this.z += deltaZ;
  }

  public void setRotation(float yaw, float pitch) {
    this.yaw = yaw;
    this.pitch = pitch;
  }

  public void setHeadRotation(float headYaw) {
    this.headYaw = headYaw;
  }

  public void setMotion(double motionX, double motionY, double motionZ) {
    this.motionX = motionX;
    this.motionY = motionY;
    this.motionZ = motionZ;
  }

  public void tick() {
    effectState.tick();
  }

  public void handleEntityEvent(EntityEvent event) {
    log.debug("Unhandled entity event for entity {}: {}", entityId, event.name());
  }

  public Vector3d originPosition(RotationOrigin origin) {
    return switch (origin) {
      case EYES -> eyePosition();
      case FEET -> pos();
    };
  }

  public boolean isEyeInFluid(ResourceKey fluid) {
    var eyePos = eyePosition();
    var breathingPos = eyePos.sub(0, BREATHING_DISTANCE_BELOW_EYES, 0);
    var breathingCoords = breathingPos.toInt();

    return level.tagsState().isFluidInTag(level.getBlockState(breathingCoords).blockType().fluidType(), fluid);
  }

  /**
   * Updates the rotation to look at a given block or location.
   *
   * @param origin   The rotation origin, either EYES or FEET.
   * @param position The block or location to look at.
   */
  public void lookAt(RotationOrigin origin, Vector3d position) {
    var originPosition = originPosition(origin);

    var dx = position.getX() - originPosition.getX();
    var dy = position.getY() - originPosition.getY();
    var dz = position.getZ() - originPosition.getZ();

    var sqr = Math.sqrt(dx * dx + dz * dz);

    this.pitch =
      MathHelper.wrapDegrees((float) (-(Math.atan2(dy, sqr) * 180.0F / (float) Math.PI)));
    this.yaw =
      MathHelper.wrapDegrees((float) (Math.atan2(dz, dx) * 180.0F / (float) Math.PI) - 90.0F);
  }

  public double eyeHeight() {
    return 1.62F;
  }

  public Vector3d eyePosition() {
    return Vector3d.from(x, y + eyeHeight(), z);
  }

  public Vector3d rotationVector() {
    var yawRadians = (float) Math.toRadians(yaw);
    var pitchRadians = (float) Math.toRadians(pitch);
    var x = -Math.sin(yawRadians) * Math.cos(pitchRadians);
    var y = -Math.sin(pitchRadians);
    var z = Math.cos(yawRadians) * Math.cos(pitchRadians);
    return Vector3d.from(x, y, z);
  }

  public Vector3i blockPos() {
    return Vector3i.from(x, y, z);
  }

  public Vector3d pos() {
    return Vector3d.from(x, y, z);
  }

  public float width() {
    return entityType.width();
  }

  public float height() {
    return entityType.height();
  }

  public AABB boundingBox() {
    return boundingBox(x, y, z);
  }

  public AABB boundingBox(Vector3d pos) {
    return boundingBox(pos.getX(), pos.getY(), pos.getZ());
  }

  public AABB boundingBox(double x, double y, double z) {
    var w = width() / 2F;
    var h = height();
    return new AABB(x - w, y, z - w, x + w, y + h, z + w);
  }

  public double attributeValue(AttributeType type) {
    return attributeState.getOrCreateAttribute(type).calculateValue();
  }
}
