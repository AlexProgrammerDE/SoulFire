package org.spongepowered.asm.mixin.transformer;

import org.spongepowered.asm.service.ILegacyClassTransformer;

public final class AgentMixinProxy implements ILegacyClassTransformer {
  private static final MixinTransformer transformer = new MixinTransformer();

  @Override
  public String getName() {
    return this.getClass().getName();
  }

  @Override
  public boolean isDelegationExcluded() {
    return true;
  }

  @Override
  public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
    return AgentMixinProxy.transformer.transformClassBytes(name, transformedName, basicClass);
  }
}
