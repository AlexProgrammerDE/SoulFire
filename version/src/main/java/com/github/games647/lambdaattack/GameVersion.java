package com.github.games647.lambdaattack;

public enum GameVersion {

    VERSION_1_7,
    VERSION_1_8,
    VERSION_1_9,
    VERSION_1_10,
    VERSION_1_11;

    public static GameVersion findByName(String name) {
        switch (name) {
            case "1.7":
                return VERSION_1_7;
            case "1.8":
                return VERSION_1_8;
            case "1.9":
                return VERSION_1_9;
            case "1.10":
                return VERSION_1_10;
            case "1.11":
                return VERSION_1_11;
        }

        return null;
    }
}
