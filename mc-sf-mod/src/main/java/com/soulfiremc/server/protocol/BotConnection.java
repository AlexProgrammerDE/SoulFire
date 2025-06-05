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
package com.soulfiremc.server.protocol;

import com.google.common.collect.Queues;
import com.soulfiremc.mod.access.IMinecraft;
import com.soulfiremc.mod.util.SFConstants;
import com.soulfiremc.mod.util.SFModHelpers;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireScheduler;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.account.service.BedrockData;
import com.soulfiremc.server.account.service.OfflineJavaData;
import com.soulfiremc.server.account.service.OnlineChainJavaData;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.PreBotConnectEvent;
import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.lib.InstanceSettingsSource;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.log4j.SFLogAppender;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.EventLoopGroup;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.reflect.Fields;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.util.thread.BlockableEventLoop;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Getter
public final class BotConnection {
  public static final ThreadLocal<BotConnection> CURRENT = new InheritableThreadLocal<>();
  private final List<Runnable> shutdownHooks = new CopyOnWriteArrayList<>();
  private final Queue<Runnable> preTickHooks = new ConcurrentLinkedQueue<>();
  private final MetadataHolder metadata = new MetadataHolder();
  private final ControlState controlState = new ControlState();
  private final BotControlAPI botControl = new BotControlAPI();
  private final SoulFireScheduler scheduler;
  private final BotConnectionFactory factory;
  private final InstanceManager instanceManager;
  private final InstanceSettingsSource settingsSource;
  private final MinecraftAccount minecraftAccount;
  private final UUID accountProfileId;
  private final String accountName;
  private final ServerAddress serverAddress;
  private final SoulFireScheduler.RunnableWrapper runnableWrapper;
  private final Object shutdownLock = new Object();
  private final Minecraft minecraft;
  @Nullable
  private final SFProxy proxy;
  private final EventLoopGroup eventLoopGroup;
  @Setter
  private ProtocolVersion currentProtocolVersion;
  private boolean explicitlyShutdown = false;
  @Setter
  private boolean pause = false;

  public BotConnection(
    BotConnectionFactory factory,
    InstanceManager instanceManager,
    InstanceSettingsSource settingsSource,
    MinecraftAccount minecraftAccount,
    ProtocolVersion currentProtocolVersion,
    ServerAddress serverAddress,
    @Nullable
    SFProxy proxyData,
    EventLoopGroup eventLoopGroup) {
    this.factory = factory;
    this.instanceManager = instanceManager;
    this.settingsSource = settingsSource;
    this.minecraftAccount = minecraftAccount;
    this.accountProfileId = minecraftAccount.profileId();
    this.accountName = minecraftAccount.lastKnownName();
    this.runnableWrapper = instanceManager.runnableWrapper().with(new BotRunnableWrapper(this));
    this.scheduler = new SoulFireScheduler(runnableWrapper);
    this.serverAddress = serverAddress;
    this.minecraft = createMinecraftCopy();
    this.proxy = proxyData;
    this.eventLoopGroup = eventLoopGroup;
    this.currentProtocolVersion = currentProtocolVersion;
  }

  @SneakyThrows
  private Minecraft createMinecraftCopy() {
    var newInstance = SFModHelpers.deepCopy(SFConstants.BASE_MC_INSTANCE);

    Fields.set(newInstance, Minecraft.class.getDeclaredField("progressTasks"), Queues.newConcurrentLinkedQueue());
    Fields.set(newInstance, BlockableEventLoop.class.getDeclaredField("pendingRunnables"), Queues.newConcurrentLinkedQueue());
    Fields.set(newInstance, Minecraft.class.getDeclaredField("toastManager"), new ToastManager(newInstance));
    Fields.set(newInstance, Minecraft.class.getDeclaredField("gui"), new Gui(newInstance));
    Fields.set(newInstance, Minecraft.class.getDeclaredField("running"), true);
    Fields.set(newInstance, Minecraft.class.getDeclaredField("user"), new User(
      minecraftAccount.lastKnownName(),
      minecraftAccount.profileId(),
      switch (minecraftAccount.accountData()) {
        case BedrockData ignored -> "bedrock";
        case OfflineJavaData ignored -> "offline";
        case OnlineChainJavaData onlineChainJavaData -> onlineChainJavaData.authToken();
      },
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

    ((IMinecraft) newInstance).soulfire$setConnection(this);

    return newInstance;
  }

  public CompletableFuture<?> connect() {
    return scheduler.runAsync(
      () -> {
        SoulFireAPI.postEvent(new PreBotConnectEvent(this));
        minecraft.execute(runnableWrapper.wrap(() -> ConnectScreen.startConnecting(
          new JoinMultiplayerScreen(new TitleScreen()),
          minecraft,
          serverAddress,
          new ServerData("soulfire", "foo", ServerData.Type.OTHER),
          false,
          null
        )));

        scheduler.runAsync(() -> {
          SFConstants.MINECRAFT_INSTANCE.set(minecraft);
          minecraft.run();
        });
      });
  }

  public void gracefulDisconnect() {
    synchronized (shutdownLock) {
      if (!minecraft.isRunning()) {
        return;
      }

      minecraft.executeBlocking(() -> {
        if (minecraft.level != null) {
          minecraft.level.disconnect();
        }

        minecraft.disconnect();
      });

      minecraft.stop();

      explicitlyShutdown = true;

      // Run all shutdown hooks
      shutdownHooks.forEach(Runnable::run);

      // Shut down all executors
      scheduler.shutdown();
    }
  }

  public void wasDisconnected() {
    synchronized (shutdownLock) {
      if (!minecraft.isRunning()) {
        return;
      }

      minecraft.executeBlocking(() -> {
        if (minecraft.level != null) {
          minecraft.level.disconnect();
        }

        minecraft.disconnect();
      });

      minecraft.stop();

      // Run all shutdown hooks
      shutdownHooks.forEach(Runnable::run);

      // Shut down all executors
      scheduler.shutdown();
    }
  }

  public void sendChatMessage(String message) {
    if (minecraft.player == null) {
      return;
    }

    var chatScreen = new ChatScreen("");
    chatScreen.init(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    chatScreen.handleChatInput(message, false);
  }

  private record BotRunnableWrapper(BotConnection botConnection) implements SoulFireScheduler.RunnableWrapper {
    @Override
    public Runnable wrap(Runnable runnable) {
      return () -> {
        try (
          var ignored1 = SFHelpers.smartThreadLocalCloseable(CURRENT, botConnection);
          var ignored2 = SFHelpers.smartMDCCloseable(SFLogAppender.SF_BOT_ACCOUNT_ID, botConnection.accountProfileId().toString());
          var ignored3 = SFHelpers.smartMDCCloseable(SFLogAppender.SF_BOT_ACCOUNT_NAME, botConnection.accountName())) {
          runnable.run();
        }
      };
    }
  }
}
