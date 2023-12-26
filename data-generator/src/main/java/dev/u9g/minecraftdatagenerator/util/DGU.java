package dev.u9g.minecraftdatagenerator.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Language;
import net.minecraft.world.World;

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
        return language.get(translationKey);
    }

    public static String translateText(String translationKey) {
        return translateTextFallback(translationKey);
    }

    public static World getWorld() {
        return getCurrentlyRunningServer().getOverworld();
    }
}
