package net.pistonmaster.serverwrecker.pathfinding;

import java.util.Set;


public interface Graph<T extends GraphNode> {
    Set<T> getConnections(T node);
}
