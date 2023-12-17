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
package net.pistonmaster.serverwrecker.server.viaversion.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import net.pistonmaster.serverwrecker.server.settings.BotSettings;
import net.pistonmaster.serverwrecker.server.viaversion.StorableSettingsHolder;

import java.util.Objects;

public class SWViaVersionProvider implements VersionProvider {

    @Override
    public int getClosestServerProtocol(UserConnection connection) {
        StorableSettingsHolder settingsHolder = connection.get(StorableSettingsHolder.class);
        Objects.requireNonNull(settingsHolder, "StorableOptions is null");

        return settingsHolder.settingsHolder().get(BotSettings.PROTOCOL_VERSION, ProtocolVersion::getClosest).getVersion();
    }
}