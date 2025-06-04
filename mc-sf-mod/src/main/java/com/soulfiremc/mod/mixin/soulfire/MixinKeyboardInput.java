package com.soulfiremc.mod.mixin.soulfire;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.soulfiremc.server.protocol.BotConnection;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
  @WrapOperation(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
  private Input soulfire$updatePlayerMoveState(boolean bl, boolean bl2, boolean bl3, boolean bl4, boolean bl5, boolean bl6, boolean bl7, Operation<Input> original) {
    var controlState = BotConnection.CURRENT.get().controlState();
    return new Input(
      controlState.up(),
      controlState.down(),
      controlState.left(),
      controlState.right(),
      controlState.jump(),
      controlState.shift(),
      controlState.sprint()
    );
  }
}
