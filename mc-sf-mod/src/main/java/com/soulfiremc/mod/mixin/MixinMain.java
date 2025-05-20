/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.mod.mixin;

import com.google.common.collect.Queues;
import com.soulfiremc.mod.util.SFModHelpers;
import com.soulfiremc.mod.util.SFModThreadLocals;
import lombok.SneakyThrows;
import me.earth.headlessmc.lwjgl.agent.LwjglAgent;
import net.lenni0451.reflect.Agents;
import net.lenni0451.reflect.Fields;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.main.Main;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Mixin(Main.class)
public final class MixinMain {
  @SneakyThrows
  @Inject(method = "main([Ljava/lang/String;)V", at = @At("HEAD"))
  private static void init(CallbackInfo cir) {
    Agents.getInstrumentation().addTransformer(new LwjglAgent());
  }

  @SneakyThrows
  @Redirect(method = "main([Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;run()V"))
  private static void init(Minecraft old) {
    IntStream.range(1, 2).forEach(i -> tryConnect(createMinecraftCopy(old, "Bot_" + i)));

    while (true) {
      TimeUnit.SECONDS.sleep(1);
    }
  }

  @SneakyThrows
  @Redirect(method = "main([Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;destroy()V"))
  private static void preventDestroy(Minecraft instance) {
  }

  @SneakyThrows
  @Redirect(method = "main([Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;stop()V"))
  private static void preventStop(Minecraft instance) {
  }

  @Unique
  @SneakyThrows
  @SuppressWarnings("ResultOfMethodCallIgnored")
  private static Thread tryConnect(Minecraft minecraft) {
    minecraft.submit(() -> {
      var ip = "127.0.0.1:25565";
      var serverAddress = ServerAddress.parseString(ip);
      ConnectScreen.startConnecting(
        new JoinMultiplayerScreen(new TitleScreen()),
        minecraft,
        serverAddress,
        new ServerData("test", ip, ServerData.Type.OTHER),
        false,
        null
      );
      var connection = new Connection(PacketFlow.CLIENTBOUND);
      Connection.connect(
        ServerNameResolver.DEFAULT.resolveAddress(serverAddress)
          .map(ResolvedServerAddress::asInetSocketAddress)
          .orElseThrow(),
        true,
        connection
      );
    });

    return Thread.ofVirtual().start(() -> {
      try {
        SFModThreadLocals.MINECRAFT_INSTANCE.set(minecraft);
        minecraft.run();
      } catch (Throwable t) {
        t.printStackTrace();
      }
    });
  }

  @Unique
  @SneakyThrows
  private static Minecraft createMinecraftCopy(Minecraft old, String username) {
    var newInstance = SFModHelpers.deepCopy(old);

    Fields.set(newInstance, Minecraft.class.getDeclaredField("progressTasks"), Queues.newConcurrentLinkedQueue());
    Fields.set(newInstance, BlockableEventLoop.class.getDeclaredField("pendingRunnables"), Queues.newConcurrentLinkedQueue());
    Fields.set(newInstance, Minecraft.class.getDeclaredField("toastManager"), new ToastManager(newInstance));
    Fields.set(newInstance, Minecraft.class.getDeclaredField("gui"), new Gui(newInstance));
    Fields.set(newInstance, Minecraft.class.getDeclaredField("user"), new User(
      username,
      UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8)),
      "offline",
      Optional.empty(),
      Optional.empty(),
      User.Type.MSA
    ));

    {
      var getTickTargetMillis = Minecraft.class.getDeclaredMethod("getTickTargetMillis", float.class);
      getTickTargetMillis.setAccessible(true);

      Fields.set(newInstance, Minecraft.class.getDeclaredField("deltaTracker"), new DeltaTracker.Timer(20.0F, 0L, (f) -> {
        try {
          return (float) getTickTargetMillis.invoke(newInstance, f);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }));
    }

    return newInstance;
  }
}
