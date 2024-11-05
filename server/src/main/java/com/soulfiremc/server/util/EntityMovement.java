package com.soulfiremc.server.util;

import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;

import java.util.List;

public record EntityMovement(Vector3d pos, Vector3d deltaMovement, float yRot, float xRot) {
  public static EntityMovement toAbsolute(EntityMovement current, EntityMovement packet, List<PositionElement> relative) {
    var x = relative.contains(PositionElement.X) ? current.pos().getX() + packet.pos().getX() : packet.pos().getX();
    var y = relative.contains(PositionElement.Y) ? current.pos().getY() + packet.pos().getY() : packet.pos().getY();
    var z = relative.contains(PositionElement.Z) ? current.pos().getZ() + packet.pos().getZ() : packet.pos().getZ();
    var yRot =
      relative.contains(PositionElement.Y_ROT)
        ? current.yRot() + packet.yRot()
        : packet.yRot();
    var xRot =
      relative.contains(PositionElement.X_ROT)
        ? current.xRot() + packet.xRot()
        : packet.xRot();
    var deltaMovement = packet.deltaMovement();
    if (relative.contains(PositionElement.ROTATE_DELTA)) {
      var k = current.yRot() - yRot;
      var l = current.xRot() - xRot;
      deltaMovement = VectorHelper.xRot(deltaMovement, (float) Math.toRadians(l));
      deltaMovement = VectorHelper.yRot(deltaMovement, (float) Math.toRadians(k));
    }

    return new EntityMovement(Vector3d.from(x, y, z), Vector3d.from(
      relative.contains(PositionElement.DELTA_X) ? current.deltaMovement().getX() + deltaMovement.getX() : deltaMovement.getX(),
      relative.contains(PositionElement.DELTA_Y) ? current.deltaMovement().getY() + deltaMovement.getY() : deltaMovement.getY(),
      relative.contains(PositionElement.DELTA_Z) ? current.deltaMovement().getZ() + deltaMovement.getZ() : deltaMovement.getZ()
    ), yRot, xRot);
  }
}
