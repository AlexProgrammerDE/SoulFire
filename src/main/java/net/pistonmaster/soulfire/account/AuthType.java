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
package net.pistonmaster.soulfire.account;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.soulfire.account.service.*;

@Getter
@RequiredArgsConstructor
public enum AuthType {
    MICROSOFT_JAVA("Microsoft Java", OnlineJavaData.class, new SWJavaMicrosoftAuthService()),
    MICROSOFT_BEDROCK("Microsoft Bedrock", BedrockData.class, new SWBedrockMicrosoftAuthService()),
    THE_ALTENING("The Altening", OnlineJavaData.class, new SWTheAlteningAuthService()),
    EASYMC("EasyMC", OnlineJavaData.class, new SWEasyMCAuthService()),
    OFFLINE("Offline", OfflineJavaData.class, new SWOfflineAuthService());

    private final String displayName;
    private final Class<? extends AccountData> accountDataClass;
    private final MCAuthService<?> authService;

    @Override
    public String toString() {
        return displayName;
    }
}
