package net.pistonmaster.wirebot.common;

public enum ProxyType {
    HTTP(),
    SOCKS4(),
    SOCKS5();

    public static ProxyType findByName(String name) {
        for (ProxyType version : values()) {
            if (version.name().equals(name)) {
                return version;
            }
        }

        return null;
    }
}
