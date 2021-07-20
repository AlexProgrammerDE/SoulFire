package net.pistonmaster.wirebot.api;

import net.pistonmaster.wirebot.WireBot;

import javax.swing.*;

public class WireBotAPI {
    private static final WireBot wireBpt = WireBot.getInstance();

    public static JFrame getWindow() {
        return wireBpt.getWindow();
    }
}
