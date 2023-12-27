package net.pistonmaster.serverwrecker.generator.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockBehaviour.class)
public interface BlockAccessor {
    @Accessor(value = "properties", remap = false)
    BlockBehaviour.Properties properties();
}
