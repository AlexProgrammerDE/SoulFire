/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.server.viaversion;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.CompressionProvider;
import net.pistonmaster.serverwrecker.server.viaversion.providers.*;
import net.raphimc.viabedrock.protocol.providers.NettyPipelineProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_3_1_2to1_2_4_5.providers.OldAuthProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.providers.EncryptionProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_8to1_7_6_10.providers.GameProfileFetcher;

public class SWViaLoader implements ViaPlatformLoader {
    @Override
    public void load() {
        Via.getManager().getProviders().use(VersionProvider.class, new SWViaVersionProvider());
        Via.getManager().getProviders().use(CompressionProvider.class, new SWViaCompressionProvider());

        // For ViaLegacy
        Via.getManager().getProviders().use(GameProfileFetcher.class, new SWViaGameProfileFetcher());
        Via.getManager().getProviders().use(EncryptionProvider.class, new SWViaEncryptionProvider());
        Via.getManager().getProviders().use(OldAuthProvider.class, new SWViaOldAuthProvider());

        // For ViaBedrock
        Via.getManager().getProviders().use(NettyPipelineProvider.class, new SWViaNettyPipelineProvider());
    }

    @Override
    public void unload() {
    }
}
