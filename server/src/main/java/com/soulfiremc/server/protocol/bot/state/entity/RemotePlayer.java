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

import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.state.Level;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;

public class RemotePlayer extends AbstractClientPlayer {
  private Vector3d lerpDeltaMovement = Vector3d.ZERO;
  private int lerpDeltaMovementSteps;

  public RemotePlayer(BotConnection connection, Level level, GameProfile gameProfile) {
    super(connection, level, gameProfile);
    this.noPhysics = true;
  }

  @Override
  public void aiStep() {
    if (this.lerpSteps > 0) {
      this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
      this.lerpSteps--;
    }

    if (this.lerpDeltaMovementSteps > 0) {
      this.addDeltaMovement(
        Vector3d.from(
          (this.lerpDeltaMovement.getX() - this.getDeltaMovement().getX()) / (double) this.lerpDeltaMovementSteps,
          (this.lerpDeltaMovement.getY() - this.getDeltaMovement().getY()) / (double) this.lerpDeltaMovementSteps,
          (this.lerpDeltaMovement.getZ() - this.getDeltaMovement().getZ()) / (double) this.lerpDeltaMovementSteps
        )
      );
      this.lerpDeltaMovementSteps--;
    }

    this.pushEntities();
  }

  @Override
  public void lerpMotion(double x, double y, double z) {
    this.lerpDeltaMovement = Vector3d.from(x, y, z);
    this.lerpDeltaMovementSteps = this.entityType().updateInterval() + 1;
  }

  @Override
  protected void updatePlayerPose() {
  }

  @Override
  public void fromAddEntityPacket(ClientboundAddEntityPacket packet) {
    super.fromAddEntityPacket(packet);
    this.setOldPosAndRot();
  }

  @Override
  public boolean hurtClient() {
    return true;
  }
}
