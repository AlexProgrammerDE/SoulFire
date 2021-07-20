package net.pistonmaster.wirebot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URI;

@RequiredArgsConstructor
@Getter
public enum AuthServers {
    MOJANG(URI.create("https://authserver.mojang.com/")),
    THE_ALTENING(URI.create("http://authserver.thealtening.com/"));

    private final URI server;
}
