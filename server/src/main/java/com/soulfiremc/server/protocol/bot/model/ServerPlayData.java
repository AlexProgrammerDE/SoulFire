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
package com.soulfiremc.server.protocol.bot.model;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public record ServerPlayData(
  Component motd, byte @Nullable [] iconBytes) {
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ServerPlayData(var otherMotd, var otherIconBytes))) return false;
    return Objects.equals(motd, otherMotd) && Objects.deepEquals(iconBytes, otherIconBytes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(motd, Arrays.hashCode(iconBytes));
  }
}
