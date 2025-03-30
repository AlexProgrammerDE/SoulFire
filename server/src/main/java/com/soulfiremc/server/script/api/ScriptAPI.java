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
import com.soulfiremc.server.script.ScriptHelper;
import com.soulfiremc.server.script.ScriptManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

public class ScriptAPI {
  private final ScriptInfoAPI script;
  private final ScriptEventAPI event;
  private final ScriptInstanceAPI instance;

  public ScriptAPI(ScriptManager.Script script, InstanceManager instanceManager) {
    this.script = new ScriptInfoAPI(script);
    this.event = new ScriptEventAPI();
    this.instance = new ScriptInstanceAPI(instanceManager);
  }

  @HostAccess.Export
  public ScriptInfoAPI getScript() {
    return script;
  }

  @HostAccess.Export
  public ScriptEventAPI getEvent() {
    return event;
  }

  @HostAccess.Export
  public ScriptInstanceAPI getInstance() {
    return instance;
  }

  @HostAccess.Export
  public String componentToLegacySection(Component component) {
    return LegacyComponentSerializer.legacySection().serialize(component);
  }

  @HostAccess.Export
  public Value componentFromLegacySection(String legacy) {
    return ScriptHelper.componentToValue(LegacyComponentSerializer.legacySection().deserialize(legacy));
  }

  @HostAccess.Export
  public String componentToLegacyAmpersand(Component component) {
    return LegacyComponentSerializer.legacyAmpersand().serialize(component);
  }

  @HostAccess.Export
  public Value componentFromLegacyAmpersand(String legacy) {
    return ScriptHelper.componentToValue(LegacyComponentSerializer.legacyAmpersand().deserialize(legacy));
  }

  @HostAccess.Export
  public String componentToPlain(Component component) {
    return PlainTextComponentSerializer.plainText().serialize(component);
  }

  @HostAccess.Export
  public Value componentFromPlain(String plain) {
    return ScriptHelper.componentToValue(PlainTextComponentSerializer.plainText().deserialize(plain));
  }
}
