package net.pistonmaster.wirebot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URI;

@RequiredArgsConstructor
@Getter
public enum ServiceServer {
    MOJANG("Mojang", URI.create("https://authserver.mojang.com/"), URI.create("https://sessionserver.mojang.com/session/minecraft/")),
    THE_ALTENING("The Altening", URI.create("http://authserver.thealtening.com/"), URI.create("http://sessionserver.thealtening.com/"));

    private final String name;
    private final URI auth;
    private final URI session;

    @Override
    public String toString() {
        return name;
    }
}
