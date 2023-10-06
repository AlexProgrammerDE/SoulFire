package net.pistonmaster.serverwrecker.pathfinding.graph;

import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.function.Function;

@RequiredArgsConstructor
public enum SWDirection {
    DOWN(Direction.DOWN, pos -> pos.sub(Vector3i.from(0, 1, 0))),
    UP(Direction.UP, pos -> pos.add(Vector3i.from(0, -1, 0))),
    NORTH(Direction.NORTH, pos -> pos.sub(Vector3i.from(0, 0, 1))),
    SOUTH(Direction.SOUTH, pos -> pos.add(Vector3i.from(0, 0, -1))),
    WEST(Direction.WEST, pos -> pos.sub(Vector3i.from(1, 0, 0))),
    EAST(Direction.EAST, pos -> pos.add(Vector3i.from(-1, 0, 0)));

    public static final SWDirection[] VALUES = values();

    @Getter
    private final Direction direction;
    private final Function<Vector3i, Vector3i> offsetFunction;

    public Vector3i offset(Vector3i pos) {
        return offsetFunction.apply(pos);
    }
}
