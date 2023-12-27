package net.pistonmaster.serverwrecker.generator.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public class DGU {
    @SuppressWarnings("deprecation")
    public static MinecraftServer getCurrentlyRunningServer() {
        return (MinecraftServer) FabricLoader.getInstance().getGameInstance();
    }
}
