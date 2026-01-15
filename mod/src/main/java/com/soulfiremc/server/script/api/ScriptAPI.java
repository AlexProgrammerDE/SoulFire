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

import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.adventure.SoulFireAdventure;
import com.soulfiremc.server.script.ScriptHelper;
import com.soulfiremc.server.script.ScriptManager;
import net.kyori.adventure.text.Component;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

public class ScriptAPI {
  @HostAccess.Export
  public final ScriptInfoAPI script;
  @HostAccess.Export
  public final ScriptEventAPI event;
  @HostAccess.Export
  public final ScriptInstanceAPI instance;

  public ScriptAPI(Context context, ScriptManager.Script script, InstanceManager instanceManager) {
    this.script = new ScriptInfoAPI(script);
    this.event = new ScriptEventAPI(context);
    this.instance = new ScriptInstanceAPI(instanceManager);
  }

  @HostAccess.Export
  public String componentToLegacySection(Component component) {
    return SoulFireAdventure.LEGACY_SECTION_MESSAGE_SERIALIZER.serialize(component);
  }

  @HostAccess.Export
  public Value componentFromLegacySection(String legacy) {
    return ScriptHelper.componentToValue(Context.getCurrent(), SoulFireAdventure.LEGACY_SECTION_MESSAGE_SERIALIZER.deserialize(legacy));
  }

  @HostAccess.Export
  public String componentToLegacyAmpersand(Component component) {
    return SoulFireAdventure.LEGACY_AMPERSAND_MESSAGE_SERIALIZER.serialize(component);
  }

  @HostAccess.Export
  public Value componentFromLegacyAmpersand(String legacy) {
    return ScriptHelper.componentToValue(Context.getCurrent(), SoulFireAdventure.LEGACY_AMPERSAND_MESSAGE_SERIALIZER.deserialize(legacy));
  }

  @HostAccess.Export
  public String componentToPlain(Component component) {
    return SoulFireAdventure.PLAIN_MESSAGE_SERIALIZER.serialize(component);
  }

  @HostAccess.Export
  public Value componentFromPlain(String plain) {
    return ScriptHelper.componentToValue(Context.getCurrent(), SoulFireAdventure.PLAIN_MESSAGE_SERIALIZER.deserialize(plain));
  }
}
