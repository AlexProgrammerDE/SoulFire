/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
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
package net.pistonmaster.serverwrecker.util.reference;

/**
 * We don't want to use AtomicDouble because it is slower.
 * This implementation doesn't care about thread safety.
 */
public class ReferenceDouble {
    private double value;

    public ReferenceDouble() {
        this(0);
    }

    public ReferenceDouble(double value) {
        this.value = value;
    }

    public void add(double amount) {
        value += amount;
    }

    public double get() {
        return value;
    }
}
