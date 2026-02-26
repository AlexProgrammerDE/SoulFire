package com.soulfiremc.server.api.event.bot;

import com.soulfiremc.server.api.event.SoulFireBotEvent;
import com.soulfiremc.server.bot.BotConnection;

/// This event is called when the bot opens screen.
public record BotOpenContainerEvent(
  BotConnection connection,
  int containerId,
  String containerName,
  String containerType
) implements SoulFireBotEvent {}
