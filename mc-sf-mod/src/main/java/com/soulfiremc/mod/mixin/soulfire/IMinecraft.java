package com.soulfiremc.mod.mixin.soulfire;

import com.soulfiremc.server.protocol.BotConnection;

public interface IMinecraft {
  BotConnection soulfire$getConnection();

  void soulfire$setConnection(BotConnection connection);
}
