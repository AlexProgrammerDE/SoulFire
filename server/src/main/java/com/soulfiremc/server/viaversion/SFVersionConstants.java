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

import com.viaversion.viaaprilfools.api.AprilFoolsProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;

public class SFVersionConstants {
  public static final ProtocolVersion CURRENT_PROTOCOL_VERSION =
    ProtocolVersion.getProtocol(MinecraftCodec.CODEC.getProtocolVersion());

  private SFVersionConstants() {}

  public static boolean isLegacy(ProtocolVersion version) {
    return LegacyProtocolVersion.PROTOCOLS.contains(version);
  }

  public static boolean isBedrock(ProtocolVersion version) {
    return BedrockProtocolVersion.PROTOCOLS.contains(version);
  }

  public static boolean isAprilFools(ProtocolVersion version) {
    return AprilFoolsProtocolVersion.PROTOCOLS.contains(version);
  }
}
