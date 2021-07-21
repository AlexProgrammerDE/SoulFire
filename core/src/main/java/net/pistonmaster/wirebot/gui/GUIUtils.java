package net.pistonmaster.wirebot.gui;

import javax.swing.*;
import java.util.Arrays;

public class GUIUtils {
    public static <A> A getOfType(JPanel panel, Class<A> clazz) {
        //noinspection unchecked
        return (A) Arrays.stream(panel.getComponents()).filter(c -> c.getClass() == clazz).findFirst().orElse(null);
    }
}
