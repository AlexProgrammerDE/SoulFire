package com.github.games647.lambdaattack.common;

public interface IBot {
    void setLocation(EntitiyLocation loc);

    void setHealth(float health);

    void setFood(float food);

    void sendMessage(String message);
}
