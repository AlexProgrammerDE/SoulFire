package dev.u9g.minecraftdatagenerator.mixin;

import net.minecraft.server.Eula;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Eula.class)
public class EULAMixin {
    @Inject(method = "hasAgreedToEULA()Z", at = @At("TAIL"), cancellable = true)
    public void init(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
