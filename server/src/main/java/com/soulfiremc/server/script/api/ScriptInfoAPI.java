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

import com.soulfiremc.server.script.ScriptManager;
import org.graalvm.polyglot.HostAccess;

public class ScriptInfoAPI {
  @HostAccess.Export
  public final String id;
  @HostAccess.Export
  public final String name;
  @HostAccess.Export
  public final String dataDirectory;
  @HostAccess.Export
  public final String codeDirectory;

  public ScriptInfoAPI(ScriptManager.Script script) {
    this.id = script.scriptId().toString();
    this.name = script.name();
    this.dataDirectory = script.dataPath().toString();
    this.codeDirectory = script.codePath().toString();
  }
}
