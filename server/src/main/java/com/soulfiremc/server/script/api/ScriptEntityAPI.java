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
package com.soulfiremc.server.script.api;

import com.soulfiremc.server.protocol.bot.state.entity.Entity;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.graalvm.polyglot.HostAccess;

public class ScriptEntityAPI {
  private final Entity entity;

  public ScriptEntityAPI(Entity entity) {
    this.entity = entity;
  }

  @HostAccess.Export
  public String getType() {
    return entity.entityType().key().toString();
  }

  @HostAccess.Export
  public int getId() {
    return entity.entityId();
  }

  @HostAccess.Export
  public double getX() {
    return entity.x();
  }

  @HostAccess.Export
  public double getY() {
    return entity.y();
  }

  @HostAccess.Export
  public double getZ() {
    return entity.z();
  }

  @HostAccess.Export
  public int getBlockX() {
    return entity.blockPos().getX();
  }

  @HostAccess.Export
  public int getBlockY() {
    return entity.blockPos().getY();
  }

  @HostAccess.Export
  public int getBlockZ() {
    return entity.blockPos().getZ();
  }

  @HostAccess.Export
  public Vector3d getPos() {
    return entity.pos();
  }

  @HostAccess.Export
  public Vector3i getBlockPos() {
    return entity.blockPos();
  }
}
