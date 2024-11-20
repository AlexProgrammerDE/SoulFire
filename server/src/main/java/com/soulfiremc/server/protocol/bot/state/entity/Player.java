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
import com.soulfiremc.server.data.FluidTags;
import com.soulfiremc.server.protocol.bot.model.AbilitiesData;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.mcprotocollib.auth.GameProfile;

@Getter
@Setter
public abstract class Player extends LivingEntity {
  private final AbilitiesData abilitiesData = new AbilitiesData();
  protected final GameProfile gameProfile;
  protected boolean wasUnderwater = false;

  public Player(Level level, GameProfile gameProfile) {
    super(EntityType.PLAYER, level);
    this.gameProfile = gameProfile;
    uuid(gameProfile.getId());
  }

  @Override
  public void tick() {
    this.noPhysics = this.isSpectator();
    if (this.isSpectator()) {
      this.onGround(false);
    }

    this.wasUnderwater = this.isEyeInFluid(FluidTags.WATER);
    super.tick();

    var x = MathHelper.clamp(this.x(), -2.9999999E7, 2.9999999E7);
    var z = MathHelper.clamp(this.z(), -2.9999999E7, 2.9999999E7);
    if (x != this.x() || z != this.z()) {
      this.setPosition(x, this.y(), z);
    }
  }
}
