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
package com.soulfiremc.server.script.api;

import com.soulfiremc.server.database.ScriptEntity;
import com.soulfiremc.server.script.ScriptManager;
import org.graalvm.polyglot.HostAccess;

public record ScriptInfoAPI(ScriptManager.Script script) {
  @HostAccess.Export
  public String getId() {
    return script.scriptId().toString();
  }

  @HostAccess.Export
  public String getName() {
    return script.name();
  }

  @HostAccess.Export
  public String getDataDirectory() {
    return script.dataPath().toString();
  }

  @HostAccess.Export
  public String getCodeDirectory() {
    return script.codePath().toString();
  }

  @HostAccess.Export
  public boolean getElevatedPermissions() {
    return script.elevatedPermissions();
  }

  @HostAccess.Export
  public ScriptEntity.ScriptType getScriptType() {
    return script.scriptType();
  }
}
