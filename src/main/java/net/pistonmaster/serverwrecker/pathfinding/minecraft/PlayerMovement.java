package net.pistonmaster.serverwrecker.pathfinding.minecraft;

import org.cloudburstmc.math.vector.Vector3d;

public record PlayerMovement(Vector3d from, BasicMovementAction action) implements MinecraftAction {
    @Override
    public Vector3d getTargetPos() {
        return switch (action) {
            case FORWARD -> from.add(1, 0, 0);
            case BACKWARD -> from.add(-1, 0, 0);
            case LEFT -> from.add(0, 1, 0);
            case RIGHT -> from.add(0, -1, 0);
        };
    }
}
