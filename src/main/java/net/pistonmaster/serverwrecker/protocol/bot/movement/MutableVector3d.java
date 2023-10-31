package net.pistonmaster.serverwrecker.protocol.bot.movement;

import lombok.AllArgsConstructor;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

@AllArgsConstructor
public class MutableVector3d {
    public double x;
    public double y;
    public double z;

    public Vector3d toImmutable() {
        return Vector3d.from(x, y, z);
    }

    public Vector3i toImmutableInt() {
        return Vector3i.from(x, y, z);
    }

    public MutableVector3d offset(double x, double v, double z) {
        return new MutableVector3d(this.x + x, this.y + v, this.z + z);
    }

    public MutableVector3d floored() {
        return new MutableVector3d(Math.floor(x), Math.floor(y), Math.floor(z));
    }

    public void add(Vector3i flow) {
        this.x += flow.getX();
        this.y += flow.getY();
        this.z += flow.getZ();
    }

    public double norm() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public MutableVector3d normalize() {
        var norm = norm();
        if (norm != 0) {
            x /= norm;
            y /= norm;
            z /= norm;
        }

        return this;
    }

    public void translate(int i, int i1, int i2) {
        x += i;
        y += i1;
        z += i2;
    }
}
