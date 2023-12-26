package dev.u9g.minecraftdatagenerator.mixin;

import dev.u9g.minecraftdatagenerator.Main;
import dev.u9g.minecraftdatagenerator.generators.DataGenerators;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.MinecraftVersion;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Mixin(MinecraftDedicatedServer.class)
public class ReadyMixin {
    @Inject(method = "setupServer()Z", at = @At("TAIL"))
    private void init(CallbackInfoReturnable<Boolean> cir) {
        Main.LOGGER.info("Starting data generation!");
        String versionName = MinecraftVersion.CURRENT.getName();
        Path serverRootDirectory = DGU.getCurrentlyRunningServer().getRunDirectory().toPath().toAbsolutePath();
        Path dataDumpDirectory = serverRootDirectory.resolve("minecraft-data").resolve(versionName);
        DataGenerators.runDataGenerators(dataDumpDirectory);
        Main.LOGGER.info("Done data generation!");
        DGU.getCurrentlyRunningServer().stop(false);
    }
}
