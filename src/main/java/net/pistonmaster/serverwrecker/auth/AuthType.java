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
package net.pistonmaster.serverwrecker.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.auth.service.*;

@Getter
@RequiredArgsConstructor
public enum AuthType {
    MICROSOFT_JAVA("Microsoft Java", JavaData.class, new SWJavaMicrosoftAuthService()),
    MICROSOFT_BEDROCK("Microsoft Bedrock", BedrockData.class, new SWBedrockMicrosoftAuthService()),
    THE_ALTENING("The Altening", JavaData.class, new SWTheAlteningAuthService()),
    EASYMC("EasyMC", JavaData.class, new SWEasyMCAuthService()),
    OFFLINE("Offline", JavaData.class, new SWOfflineAuthService());

    private final String displayName;
    private final Class<? extends AccountData> accountDataClass;
    private final MCAuthService<?> authService;

    @Override
    public String toString() {
        return displayName;
    }
}
