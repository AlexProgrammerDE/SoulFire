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
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;

public class VehicleEntity extends Entity {
  public VehicleEntity(EntityType entityType, Level level) {
    super(entityType, level);
  }

  public float getDamage() {
    return this.metadataState.getMetadata(NamedEntityData.VEHICLE_ENTITY__ID_DAMAGE, MetadataTypes.FLOAT);
  }

  public void setDamage(float damage) {
    this.metadataState.setMetadata(NamedEntityData.VEHICLE_ENTITY__ID_DAMAGE, MetadataTypes.FLOAT, FloatEntityMetadata::new, damage);
  }

  public int getHurtTime() {
    return this.metadataState.getMetadata(NamedEntityData.VEHICLE_ENTITY__ID_HURT, MetadataTypes.INT);
  }

  public void setHurtTime(int hurtTime) {
    this.metadataState.setMetadata(NamedEntityData.VEHICLE_ENTITY__ID_HURT, MetadataTypes.INT, IntEntityMetadata::new, hurtTime);
  }

  public int getHurtDir() {
    return this.metadataState.getMetadata(NamedEntityData.VEHICLE_ENTITY__ID_HURTDIR, MetadataTypes.INT);
  }

  public void setHurtDir(int hurtDir) {
    this.metadataState.setMetadata(NamedEntityData.VEHICLE_ENTITY__ID_HURTDIR, MetadataTypes.INT, IntEntityMetadata::new, hurtDir);
  }
}
