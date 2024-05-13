package com.soulfiremc.server.spark;

import com.soulfiremc.brigadier.CommandSource;
import com.soulfiremc.server.plugins.ChatMessageLogger;
import com.soulfiremc.server.user.AuthenticatedUser;
import java.util.UUID;
import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.kyori.adventure.text.Component;

public class SFSparkCommandSender extends AbstractCommandSender<CommandSource> {
  public SFSparkCommandSender(final CommandSource delegate) {
    super(delegate);
  }

  @Override
  public String getName() {
    if (delegate instanceof AuthenticatedUser user) {
      return user.getUsername();
    }

    return "Console";
  }

  @Override
  public UUID getUniqueId() {
    return null;
  }

  @Override
  public void sendMessage(final Component message) {
    this.delegate.sendMessage(ChatMessageLogger.ANSI_MESSAGE_SERIALIZER.serialize(message));
  }

  @Override
  public boolean hasPermission(final String permission) {
    return true;
  }
}
