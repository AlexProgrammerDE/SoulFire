package dev.u9g.minecraftdatagenerator.mixin;

import dev.u9g.minecraftdatagenerator.util.BlockSettingsAccessor;
import net.minecraft.block.AbstractBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.Settings.class)
public class BlockSettingsOffsetMixin implements BlockSettingsAccessor {
    @Unique
    private AbstractBlock.OffsetType offsetType;

    @Inject(method = "offset(Lnet/minecraft/block/AbstractBlock$OffsetType;)Lnet/minecraft/block/AbstractBlock$Settings;", at = @At("HEAD"))
    public void init(AbstractBlock.OffsetType offsetType, CallbackInfoReturnable<AbstractBlock.Settings> cir) {
        this.offsetType = offsetType;
    }

    @Override
    public AbstractBlock.OffsetType getOffsetType() {
        return offsetType;
    }
}
