package dev.u9g.minecraftdatagenerator.mixin;

import net.minecraft.block.AbstractBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractBlock.class)
public interface BlockAccessor {
    @Accessor(value = "settings", remap = false)
    AbstractBlock.Settings settings();
}
