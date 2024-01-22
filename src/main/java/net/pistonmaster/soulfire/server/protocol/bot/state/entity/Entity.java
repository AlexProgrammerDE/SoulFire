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
package net.pistonmaster.soulfire.server.protocol.bot.state.entity;

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.server.data.EntityType;
import net.pistonmaster.soulfire.server.protocol.bot.movement.AABB;
import net.pistonmaster.soulfire.server.protocol.bot.state.EntityAttributeState;
import net.pistonmaster.soulfire.server.protocol.bot.state.EntityEffectState;
import net.pistonmaster.soulfire.server.protocol.bot.state.EntityMetadataState;
import net.pistonmaster.soulfire.server.util.MathHelper;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

@Slf4j
@Data
public abstract class Entity {
    private final EntityMetadataState metadataState = new EntityMetadataState();
    private final EntityAttributeState attributeState = new EntityAttributeState();
    private final EntityEffectState effectState = new EntityEffectState();
    private final int entityId;
    private final EntityType entityType;
    protected double x;
    protected double y;
    protected double z;
    protected float yaw;
    protected float headYaw;
    protected float pitch;
    protected double motionX;
    protected double motionY;
    protected double motionZ;
    protected boolean onGround;

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

    /**
     * Updates the rotation to look at a given block or location.
     *
     * @param origin   The rotation origin, either EYES or FEET.
     * @param position The block or location to look at.
     */
    public void lookAt(RotationOrigin origin, Vector3d position) {
        var eyes = origin == RotationOrigin.EYES;

        var dx = position.getX() - x;
        var dy = position.getY() - (eyes ? y + getEyeHeight() : y);
        var dz = position.getZ() - z;

        var sqr = Math.sqrt(dx * dx + dz * dz);

        this.pitch = MathHelper.wrapDegrees((float) (-(Math.atan2(dy, sqr) * 180.0F / (float) Math.PI)));
        this.yaw = MathHelper.wrapDegrees((float) (Math.atan2(dz, dx) * 180.0F / (float) Math.PI) - 90.0F);
    }

    public double getEyeHeight() {
        return 1.62F;
    }

    public Vector3d getEyePosition() {
        return Vector3d.from(x, y + getEyeHeight(), z);
    }

    public Vector3d getRotationVector() {
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
}
