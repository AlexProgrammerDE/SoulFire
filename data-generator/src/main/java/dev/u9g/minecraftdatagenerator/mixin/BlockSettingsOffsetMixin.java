package dev.u9g.minecraftdatagenerator.mixin;

import dev.u9g.minecraftdatagenerator.util.BlockSettingsAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.Properties.class)
public class BlockSettingsOffsetMixin implements BlockSettingsAccessor {
    @Unique
    private BlockBehaviour.OffsetType offsetType;

    @Inject(method = "offsetType(Lnet/minecraft/world/level/block/state/BlockBehaviour$OffsetType;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;", at = @At("HEAD"))
    public void init(BlockBehaviour.OffsetType offsetType, CallbackInfoReturnable<BlockBehaviour.Properties> cir) {
        this.offsetType = offsetType;
    }

    @Override
    public BlockBehaviour.OffsetType getOffsetType() {
        return offsetType;
    }
}
