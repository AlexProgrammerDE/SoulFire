package net.pistonmaster.soulfire.generator.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public class MCHelper {
    public static ServerLevel getLevel() {
        return getServer().overworld();
    }

    @SuppressWarnings("deprecation")
    public static DedicatedServer getServer() {
        return (DedicatedServer) FabricLoader.getInstance().getGameInstance();
    }

    public static GameTestHelper getGameTestHelper() {
        return new GameTestHelper(null) {
            @Override
            public @NotNull ServerLevel getLevel() {
                return MCHelper.getLevel();
            }
        };
    }
}
