package dev.u9g.minecraftdatagenerator.mixin;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DiggerItem.class)
public interface MiningToolItemAccessor {

    @Accessor
    TagKey<Block> getBlocks();

    @Accessor
    float getSpeed();
}
