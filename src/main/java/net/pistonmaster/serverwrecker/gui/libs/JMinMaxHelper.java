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
package net.pistonmaster.serverwrecker.gui.libs;

import javax.swing.*;

public class JMinMaxHelper {
    /**
     * Force min to be less than max and max to be more than min by changing the other value.
     *
     * @param min The min spinner
     * @param max The max spinner
     */
    public static void applyLink(JSpinner min, JSpinner max) {
        min.addChangeListener(e -> {
            if ((int) min.getValue() > (int) max.getValue()) {
                max.setValue(min.getValue());
            }
        });

        max.addChangeListener(e -> {
            if ((int) min.getValue() > (int) max.getValue()) {
                min.setValue(max.getValue());
            }
        });
    }
}
