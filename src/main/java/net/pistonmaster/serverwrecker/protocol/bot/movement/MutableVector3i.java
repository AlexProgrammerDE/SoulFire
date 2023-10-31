package net.pistonmaster.serverwrecker.protocol.bot.movement;

import lombok.AllArgsConstructor;
import org.cloudburstmc.math.vector.Vector3i;

@AllArgsConstructor
public class MutableVector3i {
    public int x;
    public int y;
    public int z;

    public Vector3i toImmutable() {
        return Vector3i.from(x, y, z);
    }
}
