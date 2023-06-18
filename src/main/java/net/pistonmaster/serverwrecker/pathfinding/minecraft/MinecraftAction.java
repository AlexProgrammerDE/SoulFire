package net.pistonmaster.serverwrecker.pathfinding.minecraft;

import net.pistonmaster.serverwrecker.pathfinding.GraphNode;
import org.cloudburstmc.math.vector.Vector3d;

public interface MinecraftAction extends GraphNode {
    Vector3d getTargetPos();
}
