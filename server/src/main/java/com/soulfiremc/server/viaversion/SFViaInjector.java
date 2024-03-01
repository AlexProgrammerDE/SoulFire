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
package com.soulfiremc.server.viaversion;

import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.SortedSet;

public class SFViaInjector implements ViaInjector {
  @Override
  public void inject() {}

  @Override
  public void uninject() {}

  @Override
  public ProtocolVersion getServerProtocolVersion() {
    return getServerProtocolVersions().first();
  }

  @Override
  public SortedSet<ProtocolVersion> getServerProtocolVersions() {
    final SortedSet<ProtocolVersion> versions = new ObjectLinkedOpenHashSet<>();
    versions.addAll(ProtocolVersion.getProtocols());
    return versions;
  }

  @Override
  public String getEncoderName() {
    return getDecoderName();
  }

  @Override
  public String getDecoderName() {
    return "via-codec";
  }

  @Override
  public JsonObject getDump() {
    return new JsonObject();
  }
}
