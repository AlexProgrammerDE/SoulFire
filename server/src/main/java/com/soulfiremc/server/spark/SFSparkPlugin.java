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
package com.soulfiremc.server.spark;

import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.server.SoulFireServer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.platform.PlatformInfo;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class SFSparkPlugin implements SparkPlugin {
  public static SFSparkPlugin INSTANCE;
  private final Path sparkDirectory;
  private final SoulFireServer server;
  @Getter
  private SparkPlatform platform;

  public void init() {
    platform = new SparkPlatform(this);
    platform.enable();

    INSTANCE = this;
  }

  @Override
  public String getVersion() {
    return BuildData.VERSION;
  }

  @Override
  public Path getPluginDirectory() {
    return sparkDirectory;
  }

  @Override
  public String getCommandName() {
    return "spark";
  }

  @Override
  public Stream<SFSparkCommandSender> getCommandSenders() {
    return Stream.of();
  }

  @Override
  public void executeAsync(final Runnable task) {
    server.scheduler().schedule(task);
  }

  @Override
  public void log(final Level level, final String msg) {
    if (level == Level.INFO) {
      log.info(msg);
    } else if (level == Level.WARNING) {
      log.warn(msg);
    } else if (level == Level.SEVERE) {
      log.error(msg);
    } else {
      throw new IllegalArgumentException(level.getName());
    }
  }

  @Override
  public void log(Level level, String s, Throwable throwable) {
    if (level == Level.INFO) {
      log.info(s, throwable);
    } else if (level == Level.WARNING) {
      log.warn(s, throwable);
    } else if (level == Level.SEVERE) {
      log.error(s, throwable);
    } else {
      throw new IllegalArgumentException(level.getName());
    }
  }

  @Override
  public PlatformInfo getPlatformInfo() {
    return new SFSparkPlatformInfo();
  }
}
