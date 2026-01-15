/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.spark;

import com.soulfiremc.server.user.SoulFireUser;
import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.event.Level;

import java.util.UUID;

public final class SFSparkCommandSender extends AbstractCommandSender<SoulFireUser> {
  public SFSparkCommandSender(final SoulFireUser delegate) {
    super(delegate);
  }

  @Override
  public String getName() {
    return delegate.getUsername();
  }

  @Override
  public @Nullable UUID getUniqueId() {
    return null;
  }

  @Override
  public void sendMessage(final Component message) {
    this.delegate.sendMessage(Level.INFO, message);
  }

  @Override
  public boolean hasPermission(final String permission) {
    return true;
  }
}
