package net.pistonmaster.serverwrecker.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.InetSocketAddress;

@Getter
@RequiredArgsConstructor
public class BotProxy {
    private final InetSocketAddress address;
    private final String username;
    private final String password;
}
