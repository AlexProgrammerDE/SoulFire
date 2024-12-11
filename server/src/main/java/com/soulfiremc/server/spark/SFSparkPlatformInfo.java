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
import me.lucko.spark.common.platform.PlatformInfo;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;

public class SFSparkPlatformInfo implements PlatformInfo {
  @Override
  public Type getType() {
    return Type.CLIENT;
  }

  @Override
  public String getName() {
    return "SoulFire";
  }

  @Override
  public String getBrand() {
    return "SoulFire";
  }

  @Override
  public String getVersion() {
    return BuildData.VERSION;
  }

  @Override
  public String getMinecraftVersion() {
    return MinecraftCodec.CODEC.getMinecraftVersion();
  }
}
