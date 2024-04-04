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

import com.soulfiremc.server.viaversion.providers.SFViaCompressionProvider;
import com.soulfiremc.server.viaversion.providers.SFViaEncryptionProvider;
import com.soulfiremc.server.viaversion.providers.SFViaGameProfileFetcher;
import com.soulfiremc.server.viaversion.providers.SFViaNettyPipelineProvider;
import com.soulfiremc.server.viaversion.providers.SFViaOldAuthProvider;
import com.soulfiremc.server.viaversion.providers.SFViaVersionProvider;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.CompressionProvider;
import net.raphimc.viabedrock.protocol.providers.NettyPipelineProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_3_1_2to1_2_4_5.providers.OldAuthProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.providers.EncryptionProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_8to1_7_6_10.providers.GameProfileFetcher;

public class SFViaLoader implements ViaPlatformLoader {
  @Override
  public void load() {
    Via.getManager().getProviders().use(VersionProvider.class, new SFViaVersionProvider());
    Via.getManager().getProviders().use(CompressionProvider.class, new SFViaCompressionProvider());

    // For ViaLegacy
    Via.getManager().getProviders().use(GameProfileFetcher.class, new SFViaGameProfileFetcher());
    Via.getManager().getProviders().use(EncryptionProvider.class, new SFViaEncryptionProvider());
    Via.getManager().getProviders().use(OldAuthProvider.class, new SFViaOldAuthProvider());

    // For ViaBedrock
    Via.getManager()
      .getProviders()
      .use(NettyPipelineProvider.class, new SFViaNettyPipelineProvider());

    // For Forge
    Protocol1_13To1_12_2.MAPPINGS
      .getChannelMappings()
      .put("FML|HS", "fml:hs"); // Forge 1.7 - 1.12.2
    Protocol1_13To1_12_2.MAPPINGS
      .getChannelMappings()
      .put("FML|MP", "fml:mp"); // Forge 1.7 - 1.12.2
    Protocol1_13To1_12_2.MAPPINGS.getChannelMappings().put("FML", "fml:fml"); // Forge 1.7
    Protocol1_13To1_12_2.MAPPINGS.getChannelMappings().put("FORGE", "fml:forge"); // Forge
    Protocol1_13To1_12_2.MAPPINGS.getChannelMappings().put("Forge", "fml:old_forge"); // Forge
  }

  @Override
  public void unload() {}
}
