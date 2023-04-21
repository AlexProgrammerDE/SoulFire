/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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
package net.pistonmaster.serverwrecker.viaversion;

import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.fastutil.ints.IntLinkedOpenHashSet;
import com.viaversion.viaversion.libs.fastutil.ints.IntSortedSet;
import com.viaversion.viaversion.libs.gson.JsonObject;
import net.pistonmaster.serverwrecker.SWConstants;

public class SRViaInjector implements ViaInjector {
    @Override
    public void inject() {
    }

    @Override
    public void uninject() {
    }

    @Override
    public IntSortedSet getServerProtocolVersions() {
        // On client-side we can connect to any server version
        IntSortedSet versions = new IntLinkedOpenHashSet();
        versions.add(ProtocolVersion.v1_8.getOriginalVersion());
        versions.add(SWConstants.getVersionsSorted()
                .stream()
                .mapToInt(ProtocolVersion::getOriginalVersion)
                .max().getAsInt());
        return versions;
    }

    @Override
    public int getServerProtocolVersion() {
        return getServerProtocolVersions().firstInt();
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
