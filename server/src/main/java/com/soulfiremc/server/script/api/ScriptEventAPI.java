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
package com.soulfiremc.server.script.api;

import com.soulfiremc.server.api.event.EventExceptionHandler;
import com.soulfiremc.server.api.event.SoulFireEvent;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;

public class ScriptEventAPI {
  private final LambdaManager lambdaManager;

  public ScriptEventAPI() {
    this.lambdaManager = LambdaManager.threadSafe(new ASMGenerator())
      .setExceptionHandler(EventExceptionHandler.INSTANCE)
      .setEventFilter(
        (c, h) -> {
          if (SoulFireEvent.class.isAssignableFrom(c)) {
            return true;
          } else {
            throw new IllegalStateException("This event handler only accepts global events");
          }
        });
  }
}
