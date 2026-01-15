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
package com.soulfiremc.server.command.brigadier;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.soulfiremc.shared.UUIDHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public final class EntityArgumentType implements ArgumentType<EntityArgumentType.EntityPredicate> {
  public static final EntityArgumentType INSTANCE = new EntityArgumentType();

  private EntityArgumentType() {}

  public static EntityPredicate getEntityMatcher(CommandContext<?> c, String argument) {
    return c.getArgument(argument, EntityPredicate.class);
  }

  @Override
  public Collection<String> getExamples() {
    return List.of(
      "Pistonmaster",
      "b1ae0778-4817-436c-96a3-a72c67cda060",
      "b1ae07784817436c96a3a72c67cda060"
    );
  }

  @Override
  public EntityPredicate parse(StringReader stringReader) throws CommandSyntaxException {
    var input = stringReader.readString();
    var parsedUniqueId = UUIDHelper.tryParseUniqueId(input);

    return entity -> {
      if (parsedUniqueId.isPresent() && entity.getUUID().equals(parsedUniqueId.get())) {
        return true;
      }

      if (!(entity instanceof Player player)) {
        return false;
      }

      return player.getGameProfile().name().equalsIgnoreCase(input);
    };
  }

  public interface EntityPredicate extends Predicate<Entity> {
  }
}
