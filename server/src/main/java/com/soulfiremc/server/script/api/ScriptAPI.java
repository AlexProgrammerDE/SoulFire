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

import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.script.ScriptManager;
import org.graalvm.polyglot.HostAccess;

public class ScriptAPI {
  @HostAccess.Export
  public final ScriptInfoAPI script;
  @HostAccess.Export
  public final ScriptEventAPI event;
  @HostAccess.Export
  public final ScriptInstanceAPI instance;

  public ScriptAPI(ScriptManager.Script script, InstanceManager instanceManager) {
    this.script = new ScriptInfoAPI(script);
    this.event = new ScriptEventAPI();
    this.instance = new ScriptInstanceAPI(instanceManager);
  }
}
