package net.pistonmaster.serverwrecker.pathfinding.minecraft;

import net.pistonmaster.serverwrecker.pathfinding.Scorer;
import org.cloudburstmc.math.vector.Vector3d;

public class MovementScorer implements Scorer<MinecraftAction> {
    @Override
    public double computeCost(MinecraftAction from, MinecraftAction to) {
        Vector3d fromPos = from.getTargetPos();
        Vector3d toPos = to.getTargetPos();

        return fromPos.distance(toPos);
    }
}
