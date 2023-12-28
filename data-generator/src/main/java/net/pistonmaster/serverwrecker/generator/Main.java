package net.pistonmaster.serverwrecker.generator;

import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("serverwrecker-data-generator");
    public static MinecraftServer SERVER;

    public void onInitialize() {
    }
}
