package net.pistonmaster.wirebot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URI;

@RequiredArgsConstructor
@Getter
public enum ServiceServer {
    MOJANG(URI.create("https://authserver.mojang.com/"), URI.create("https://sessionserver.mojang.com/session/minecraft/")),
    THE_ALTENING(URI.create("http://authserver.thealtening.com/"), URI.create("http://sessionserver.thealtening.com/"));

    private final URI auth;
    private final URI session;
}
