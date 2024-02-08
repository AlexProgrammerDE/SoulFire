package net.pistonmaster.soulfire.server.util;

import net.pistonmaster.soulfire.server.protocol.bot.movement.AABB;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.List;

public record Segment(Vector3d startPoint, Vector3d endPoint) {
    public boolean intersects(List<AABB> boxes) {
        for (AABB box : boxes) {
            if (isInside(box)) { // if both points are inside the box, we can skip this box
                continue;
            }

            if (intersectsBox(box)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInside(AABB box) {
        boolean originInside = startPoint.getX() >= box.minX &&
                startPoint.getX() <= box.maxX &&
                startPoint.getY() >= box.minY &&
                startPoint.getY() <= box.maxY &&
                startPoint.getZ() >= box.minZ &&
                startPoint.getZ() <= box.maxZ;

        boolean directionInside = endPoint.getX() >= box.minX &&
                endPoint.getX() <= box.maxX &&
                endPoint.getY() >= box.minY &&
                endPoint.getY() <= box.maxY &&
                endPoint.getZ() >= box.minZ &&
                endPoint.getZ() <= box.maxZ;

        return originInside && directionInside;
    }

    private boolean intersectsBox(AABB box) {
        double tmin = (box.minX - startPoint.getX()) / (endPoint.getX() - startPoint.getX());
        double tmax = (box.maxX - startPoint.getX()) / (endPoint.getX() - startPoint.getX());

        if (tmin > tmax) {
            double temp = tmin;
            tmin = tmax;
            tmax = temp;
        }

        double tymin = (box.minY - startPoint.getY()) / (endPoint.getY() - startPoint.getY());
        double tymax = (box.maxY - startPoint.getY()) / (endPoint.getY() - startPoint.getY());

        if (tymin > tymax) {
            double temp = tymin;
            tymin = tymax;
            tymax = temp;
        }

        if ((tmin > tymax) || (tymin > tmax)) {
            return false;
        }

        if (tymin > tmin) {
            tmin = tymin;
        }

        if (tymax < tmax) {
            tmax = tymax;
        }

        double tzmin = (box.minZ - startPoint.getZ()) / (endPoint.getZ() - startPoint.getZ());
        double tzmax = (box.maxZ - startPoint.getZ()) / (endPoint.getZ() - startPoint.getZ());

        if (tzmin > tzmax) {
            double temp = tzmin;
            tzmin = tzmax;
            tzmax = temp;
        }

        return (!(tmin > tzmax)) && (!(tzmin > tmax));
    }
}
