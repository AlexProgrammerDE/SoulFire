/*
 * ServerWrecker
 *
 * Copyright (C) 2021 ServerWrecker
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
package net.pistonmaster.serverwrecker.version.v1_13;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import net.pistonmaster.serverwrecker.common.IPacketWrapper;

public class ProtocolWrapper1_13 extends MinecraftProtocol implements IPacketWrapper {
    public ProtocolWrapper1_13(String username) {
        super(username);
    }

    public ProtocolWrapper1_13(GameProfile profile, String clientToken, String accessToken) {
        super(profile, clientToken, accessToken);
    }

    @Override
    public String getProfileName() {
        return getProfile().getName();
    }
}
