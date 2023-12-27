package net.pistonmaster.serverwrecker.generator.mixin;

import net.pistonmaster.serverwrecker.generator.Main;
import net.pistonmaster.serverwrecker.generator.generators.DataGenerators;
import net.pistonmaster.serverwrecker.generator.util.DGU;
import net.minecraft.DetectedVersion;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Mixin(DedicatedServer.class)
public class ReadyMixin {
    @Inject(method = "initServer()Z", at = @At("TAIL"))
    private void init(CallbackInfoReturnable<Boolean> cir) {
        Main.LOGGER.info("Starting data generation!");
        String versionName = DetectedVersion.BUILT_IN.getName();
        Path serverRootDirectory = DGU.getCurrentlyRunningServer().getServerDirectory().toPath().toAbsolutePath();
        Path dataDumpDirectory = serverRootDirectory.resolve("minecraft-data").resolve(versionName);
        DataGenerators.runDataGenerators(dataDumpDirectory);
        Main.LOGGER.info("Done data generation!");
        DGU.getCurrentlyRunningServer().halt(false);
    }
}
