package net.pistonmaster.serverwrecker.gui.libs;

import javax.swing.*;

public class JMinMaxHelper {
    /**
     * Force min to be less than max and max to be more than min by changing the other value.
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
