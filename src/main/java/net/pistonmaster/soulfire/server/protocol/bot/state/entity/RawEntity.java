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

import com.github.steveice10.mc.protocol.data.game.entity.object.ObjectData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.soulfire.server.data.EntityType;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class RawEntity extends Entity {
    private final UUID uuid;
    private final ObjectData data;
    private float yaw;
    private float headYaw;
    private float pitch;

    public RawEntity(int entityId, UUID uuid, EntityType type, ObjectData data) {
        super(entityId, type);
        this.uuid = uuid;
        this.data = data;
    }
}
