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
package com.soulfiremc.server.protocol.bot.state;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import lombok.Getter;

@Getter
public class TickHookContext {
  public static final ThreadLocal<TickHookContext> INSTANCE =
    ThreadLocal.withInitial(TickHookContext::new);

  private final Multimap<HookType, Runnable> hooks =
    MultimapBuilder.enumKeys(HookType.class).arrayListValues().build();

  public void addHook(HookType type, Runnable hook) {
    hooks.put(type, hook);
  }

  public void callHooks(HookType type) {
    hooks.get(type).forEach(Runnable::run);
  }

  public void clear() {
    hooks.clear();
  }

  public enum HookType {
    PRE_TICK,
    PRE_ENTITY_TICK,
    POST_ENTITY_TICK,
    POST_TICK
  }
}
