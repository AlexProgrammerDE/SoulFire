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

import com.soulfiremc.server.viaversion.providers.*;
import com.viaversion.vialoader.impl.viaversion.VLLoader;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viaversion.protocols.v1_8to1_9.provider.CompressionProvider;
import net.raphimc.viabedrock.protocol.provider.NettyPipelineProvider;
import net.raphimc.vialegacy.protocol.release.r1_2_4_5tor1_3_1_2.provider.OldAuthProvider;
import net.raphimc.vialegacy.protocol.release.r1_6_4tor1_7_2_5.provider.EncryptionProvider;
import net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.provider.GameProfileFetcher;

public class SFVLLoaderImpl extends VLLoader {
  @Override
  public void load() {
    super.load();

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
    Protocol1_12_2To1_13.MAPPINGS
      .getChannelMappings()
      .put("FML|HS", "fml:hs"); // Forge 1.7 - 1.12.2
    Protocol1_12_2To1_13.MAPPINGS
      .getChannelMappings()
      .put("FML|MP", "fml:mp"); // Forge 1.7 - 1.12.2
    Protocol1_12_2To1_13.MAPPINGS.getChannelMappings().put("FML", "fml:fml"); // Forge 1.7
    Protocol1_12_2To1_13.MAPPINGS.getChannelMappings().put("FORGE", "fml:forge"); // Forge
    Protocol1_12_2To1_13.MAPPINGS.getChannelMappings().put("Forge", "fml:old_forge"); // Forge
  }
}
