/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.protocol.bot.state.entity;

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import lombok.Data;
import net.pistonmaster.serverwrecker.protocol.bot.state.EntityAttributesState;
import net.pistonmaster.serverwrecker.protocol.bot.state.EntityEffectState;
import net.pistonmaster.serverwrecker.protocol.bot.state.EntityMetadataState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public abstract class EntityLikeState {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityLikeState.class);
    private final EntityMetadataState metadataState = new EntityMetadataState();
    private final EntityAttributesState attributesState = new EntityAttributesState();
    private final EntityEffectState effectState = new EntityEffectState();
    private final int entityId;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float headYaw;
    private float pitch;
    private double motionX;
    private double motionY;
    private double motionZ;
    private boolean onGround;

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
        LOGGER.debug("Entity event for entity {}: {}", entityId, event.name());
    }
}
