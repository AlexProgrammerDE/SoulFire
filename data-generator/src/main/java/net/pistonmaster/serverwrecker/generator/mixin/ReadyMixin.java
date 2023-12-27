package net.pistonmaster.serverwrecker.generator.mixin;

import net.minecraft.DetectedVersion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.pistonmaster.serverwrecker.generator.Main;
import net.pistonmaster.serverwrecker.generator.generators.DataGenerators;
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
        var versionName = DetectedVersion.BUILT_IN.getName();
        var dataDumpDirectory = Path.of(System.getProperty("user.dir")).resolve("minecraft-data").resolve(versionName);
        DataGenerators.runDataGenerators(dataDumpDirectory);
        Main.LOGGER.info("Done data generation!");

        var server = (MinecraftServer) (Object) this;
        server.halt(false);
    }
}
