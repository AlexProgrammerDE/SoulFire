package com.soulfiremc.mod.access;

import com.soulfiremc.server.protocol.BotConnection;

public interface IMinecraft {
  BotConnection soulfire$getConnection();

  void soulfire$setConnection(BotConnection connection);
}
