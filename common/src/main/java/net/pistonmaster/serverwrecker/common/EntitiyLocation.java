/*
 * ServerWrecker
 *
 * Copyright (C) 2021 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.common;

import java.util.Objects;

public class EntitiyLocation {

    private final double posX;
    private final double posY;
    private final double posZ;

    private final float pitch;
    private final float yaw;

    public EntitiyLocation(double posX, double posY, double posZ, float pitch, float yaw) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public double getPosX() {
        return posX;
    }

    public double getPosY() {
        return posY;
    }

    public double getPosZ() {
        return posZ;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    @Override
    public int hashCode() {
        return Objects.hash(posX, posY, posZ, pitch, yaw);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EntitiyLocation)) {
            return false;
        }

        EntitiyLocation otherLoc = (EntitiyLocation) other;
        return Objects.equals(posX, otherLoc.posX)
                && Objects.equals(posY, otherLoc.posY)
                && Objects.equals(posZ, otherLoc.posZ)
                && Objects.equals(pitch, otherLoc.pitch)
                && Objects.equals(yaw, otherLoc.yaw);
    }


    @Override
    public String toString() {
        return "EntitiyLocation{"
                + "posX=" + posX
                + ", posY=" + posY
                + ", posZ=" + posZ
                + ", pitch=" + pitch
                + ", yaw=" + yaw
                + '}';
    }
}
