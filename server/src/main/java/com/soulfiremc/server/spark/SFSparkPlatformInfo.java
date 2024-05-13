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
  public String getVersion() {
    return BuildData.VERSION;
  }

  @Override
  public String getMinecraftVersion() {
    return MinecraftCodec.CODEC.getMinecraftVersion();
  }
}
