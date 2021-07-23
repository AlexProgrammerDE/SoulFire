package net.pistonmaster.wirebot.common;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Options {
    public final String hostname;
    public final int port;
    public final int amount;
    public final int joinDelayMs;
    public final String botNameFormat;
    public final GameVersion gameVersion;
    public final boolean autoRegister;
    public final boolean debug;
}
