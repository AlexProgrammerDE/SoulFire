package net.pistonmaster.serverwrecker.api;

import net.pistonmaster.serverwrecker.ServerWrecker;

import javax.swing.*;

public class ServerWreckerAPI {
    private static final ServerWrecker wireBpt = ServerWrecker.getInstance();

    public static JFrame getWindow() {
        return wireBpt.getWindow();
    }
}
