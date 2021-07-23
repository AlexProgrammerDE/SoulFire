package net.pistonmaster.wirebot.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractBot {
    private EntitiyLocation location;
    private float health = -1;
    private float food = -1;

    public abstract void sendMessage(String message);

    public abstract void connect(String host, int port);

    public abstract void disconnect();

    public abstract boolean isOnline();
}
