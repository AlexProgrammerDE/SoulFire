package com.soulfiremc.mod.mixin.soulfire;

import com.soulfiremc.mod.util.SFConstants;
import com.soulfiremc.server.util.netty.NettyHelper;
import io.netty.channel.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Run after VFP
@Mixin(targets = "net.minecraft.network.Connection$1", priority = 2000)
public class MixinConnection$1 {
  @Inject(method = "initChannel", at = @At(value = "HEAD"))
  private void injectProxy(Channel channel, CallbackInfo ci) {
    var proxyData = channel.attr(SFConstants.NETTY_BOT_CONNECTION).get().proxy();
    if (proxyData == null) {
      return;
    }

    NettyHelper.addProxy(proxyData, channel.pipeline());
  }
}
