package com.soulfiremc.mod.mixin.soulfire;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.soulfiremc.mod.util.SFConstants;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.util.netty.TransportHelper;
import com.viaversion.viafabricplus.injection.access.base.IClientConnection;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import net.minecraft.network.Connection;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Run after VFP
@Mixin(value = Connection.class, priority = 2000)
public class MixinConnection {
  @WrapOperation(method = "connect", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/AbstractBootstrap;"))
  private static AbstractBootstrap<?, ?> useCustomGroup(Bootstrap instance, EventLoopGroup eventExecutors, Operation<AbstractBootstrap<Bootstrap, Channel>> original, @Local(argsOnly = true) Connection clientConnection) {
    return instance
      .attr(SFConstants.NETTY_BOT_CONNECTION, BotConnection.CURRENT.get())
      .group(BotConnection.CURRENT.get().eventLoopGroup());
  }

  @WrapOperation(method = "connect", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;"))
  private static AbstractBootstrap<?, ?> useCustomChannel(Bootstrap instance, Class<? extends Channel> channelTypeClass, Operation<AbstractBootstrap<Bootstrap, Channel>> original, @Local(argsOnly = true) Connection clientConnection) {
    if (BedrockProtocolVersion.bedrockLatest.equals(((IClientConnection) clientConnection).viaFabricPlus$getTargetVersion())) {
      return instance.channelFactory(RakChannelFactory.client(TransportHelper.TRANSPORT_TYPE.datagramChannelClass()));
    } else {
      return instance.channel(TransportHelper.TRANSPORT_TYPE.socketChannelClass());
    }
  }
}
