package net.pistonmaster.serverwrecker.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URI;

@RequiredArgsConstructor
@Getter
public enum ServiceServer {
    MOJANG("Mojang", URI.create("https://authserver.mojang.com/"), URI.create("https://sessionserver.mojang.com/session/minecraft/")),
    THE_ALTENING("The Altening (1.16+ only)", URI.create("https://authserver.thealtening.com/"), URI.create("https://sessionserver.thealtening.com/"));

    private final String name;
    private final URI auth;
    private final URI session;

    @Override
    public String toString() {
        return name;
    }
}
