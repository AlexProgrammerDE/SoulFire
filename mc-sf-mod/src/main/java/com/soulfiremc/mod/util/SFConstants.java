package com.soulfiremc.mod.util;

import com.soulfiremc.server.protocol.BotConnection;
import io.netty.util.AttributeKey;
import net.minecraft.client.Minecraft;

public class SFConstants {
  public static Minecraft BASE_MC_INSTANCE;
  public static final ThreadLocal<Minecraft> MINECRAFT_INSTANCE = new InheritableThreadLocal<>();
  public static final AttributeKey<BotConnection> NETTY_BOT_CONNECTION = AttributeKey.valueOf("soulfire_bot_connection");
}
