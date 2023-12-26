package dev.u9g.minecraftdatagenerator.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.locale.Language;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public class DGU {

    @SuppressWarnings("deprecation")
    private static MinecraftServer getCurrentlyRunningServerDedicated() {
        return (MinecraftServer) FabricLoader.getInstance().getGameInstance();
    }

    public static MinecraftServer getCurrentlyRunningServer() {
        return getCurrentlyRunningServerDedicated();
    }

    private static final Language language = Language.getInstance();

    private static String translateTextFallback(String translationKey) {
        return language.getOrDefault(translationKey);
    }

    public static String translateText(String translationKey) {
        return translateTextFallback(translationKey);
    }

    public static Level getWorld() {
        return getCurrentlyRunningServer().overworld();
    }
}
