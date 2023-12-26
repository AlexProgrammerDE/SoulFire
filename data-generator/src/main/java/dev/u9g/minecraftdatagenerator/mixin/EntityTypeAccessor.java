package dev.u9g.minecraftdatagenerator.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityType.class)
public interface EntityTypeAccessor<T extends Entity> {
    @Accessor("factory")
    EntityType.EntityFactory<T> factory();
}
