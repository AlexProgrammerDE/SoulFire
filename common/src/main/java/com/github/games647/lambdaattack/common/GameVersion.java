package com.github.games647.lambdaattack.common;

public enum GameVersion {

    VERSION_1_11("1.11"),

    VERSION_1_12("1.12.2"),

    VERSION_1_14("1.14.4"),

    VERSION_1_15("1.15.2"),

    VERSION_1_16("1.16.5");

    private final String version;

    GameVersion(String version) {
        this.version = version;
    }

    public static GameVersion findByName(String name) {
        for (GameVersion version : values()) {
            if (version.version.equals(name)) {
                return version;
            }
        }

        return null;
    }


    public String getVersion() {
        return version;
    }
}
