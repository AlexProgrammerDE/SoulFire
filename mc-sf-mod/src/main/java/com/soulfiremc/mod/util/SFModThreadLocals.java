package com.soulfiremc.mod.util;

import net.minecraft.client.Minecraft;

public class SFModThreadLocals {
  public static Minecraft BASE_MC_INSTANCE;
  public static final ThreadLocal<Minecraft> MINECRAFT_INSTANCE = ThreadLocal.withInitial(() -> null);
}
