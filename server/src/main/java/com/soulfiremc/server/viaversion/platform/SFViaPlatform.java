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
package com.soulfiremc.server.viaversion.platform;

import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.server.viaversion.JLoggerToSLF4J;
import com.soulfiremc.server.viaversion.SFViaInjector;
import com.soulfiremc.server.viaversion.SFViaTask;
import com.viaversion.viaversion.ViaAPIBase;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import com.viaversion.viaversion.api.command.ViaCommandSender;
import com.viaversion.viaversion.api.configuration.ViaVersionConfig;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.platform.ViaPlatform;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.configuration.AbstractViaConfig;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.util.VersionInfo;
import io.netty.buffer.ByteBuf;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.raphimc.vialoader.commands.UserCommandSender;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class SFViaPlatform implements ViaPlatform<UserConnection> {
  private final Path dataFolder;
  private final JLoggerToSLF4J logger = new JLoggerToSLF4J(LoggerFactory.getLogger("ViaVersion"));
  private final ViaAPI<UserConnection> api =
    new ViaAPIBase<>() {
      @Override
      public ProtocolVersion getPlayerProtocolVersion(final UserConnection player) {
        return player.getProtocolInfo().protocolVersion();
      }

      @Override
      public void sendRawPacket(final UserConnection player, final ByteBuf packet) {
        player.scheduleSendRawPacket(packet);
      }
    };
  @Getter
  private final ViaInjector injector = new SFViaInjector();
  private ViaVersionConfig config;

  public void init() {
    config =
      new AbstractViaConfig(dataFolder.resolve("config.yml").toFile(), logger) {
        private static final List<String> UNSUPPORTED =
          List.of(
            "checkforupdates",
            "bungee-ping-interval",
            "bungee-ping-save",
            "bungee-servers",
            "velocity-ping-interval",
            "velocity-ping-save",
            "velocity-servers",
            "block-protocols",
            "block-disconnect-msg",
            "reload-disconnect-msg",
            "max-pps",
            "max-pps-kick-msg",
            "tracking-period",
            "tracking-warning-pps",
            "tracking-max-warnings",
            "tracking-max-kick-msg",
            "blockconnection-method",
            "quick-move-action-fix",
            "item-cache",
            "change-1_9-hitbox",
            "change-1_14-hitbox",
            "use-new-deathmessages",
            "nms-player-ticking");

        {
          this.reload();
        }

        @Override
        protected void handleConfig(Map<String, Object> config) {}

        @Override
        public List<String> getUnsupportedOptions() {
          return UNSUPPORTED;
        }

        @Override
        public boolean isCheckForUpdates() {
          return false;
        }

        @Override
        public String getBlockConnectionMethod() {
          return "packet";
        }

        @Override
        public boolean is1_12QuickMoveActionFix() {
          return false;
        }

        @Override
        public boolean isItemCache() {
          return false;
        }

        @Override
        public boolean is1_9HitboxFix() {
          return false;
        }

        @Override
        public boolean is1_14HitboxFix() {
          return false;
        }

        @Override
        public boolean isShowNewDeathMessages() {
          return false;
        }

        @Override
        public boolean isNMSPlayerTicking() {
          return false;
        }
      };
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public String getPlatformName() {
    return "SoulFire";
  }

  @Override
  public String getPlatformVersion() {
    return BuildData.VERSION;
  }

  @Override
  public boolean isProxy() {
    return true;
  }

  @Override
  public String getPluginVersion() {
    return VersionInfo.VERSION;
  }

  @Override
  public SFViaTask runAsync(Runnable runnable) {
    return new SFViaTask(Via.getManager().getScheduler().execute(runnable));
  }

  @Override
  public SFViaTask runRepeatingAsync(Runnable runnable, long period) {
    return new SFViaTask(
      Via.getManager()
        .getScheduler()
        .scheduleRepeating(runnable, 0, period * 50, TimeUnit.MILLISECONDS));
  }

  @Override
  public SFViaTask runSync(Runnable runnable) {
    return this.runAsync(runnable);
  }

  @Override
  public SFViaTask runSync(Runnable runnable, long delay) {
    return new SFViaTask(
      Via.getManager().getScheduler().schedule(runnable, delay * 50, TimeUnit.MILLISECONDS));
  }

  @Override
  public SFViaTask runRepeatingSync(Runnable runnable, long period) {
    return this.runRepeatingAsync(runnable, period);
  }

  @Override
  public ViaCommandSender[] getOnlinePlayers() {
    return Via.getManager().getConnectionManager().getConnectedClients().values().stream()
      .map(UserCommandSender::new)
      .toArray(ViaCommandSender[]::new);
  }

  @Override
  public void sendMessage(UUID uuid, String message) {
    if (uuid == null) {
      this.getLogger().info(message);
    } else {
      this.getLogger().info("[%s] %s".formatted(uuid, message));
    }
  }

  @Override
  public boolean kickPlayer(UUID uuid, String message) {
    return false;
  }

  @Override
  public boolean isPluginEnabled() {
    return true;
  }

  @Override
  public ViaAPI<UserConnection> getApi() {
    return api;
  }

  @Override
  public ViaVersionConfig getConf() {
    return config;
  }

  @Override
  public File getDataFolder() {
    return dataFolder.toFile();
  }

  @Override
  public void onReload() {}

  @Override
  public JsonObject getDump() {
    return injector.getDump();
  }

  @Override
  public boolean hasPlugin(String name) {
    return false;
  }
}
