package com.github.games647.lambdaattack;

public enum GameVersion {

    VERSION_1_11("1.11"),

    VERSION_1_12("1.12.2");

    public static GameVersion findByName(String name) {
        switch (name) {
            case "1.11":
                return VERSION_1_11;
            case "1.12":
                return VERSION_1_12;
        }

        return null;
    }

    private final String version;

    GameVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
