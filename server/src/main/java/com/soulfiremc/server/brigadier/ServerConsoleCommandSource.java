package com.soulfiremc.server.brigadier;

import com.soulfiremc.brigadier.ClientConsoleCommandSource;
import com.soulfiremc.server.user.Permission;
import com.soulfiremc.server.user.ServerCommandSource;
import java.util.UUID;
import net.kyori.adventure.util.TriState;

public class ServerConsoleCommandSource extends ClientConsoleCommandSource implements ServerCommandSource {
  private static final UUID CONSOLE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  private static final String CONSOLE_NAME = "CONSOLE";
  public static final ServerConsoleCommandSource INSTANCE = new ServerConsoleCommandSource();

  @Override
  public UUID getUniqueId() {
    return CONSOLE_UUID;
  }

  @Override
  public String getUsername() {
    return CONSOLE_NAME;
  }

  @Override
  public TriState getPermission(Permission permission) {
    return TriState.TRUE;
  }
}
