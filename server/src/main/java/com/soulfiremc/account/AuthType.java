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
package com.soulfiremc.account;

import com.soulfiremc.account.service.AccountData;
import com.soulfiremc.account.service.BedrockData;
import com.soulfiremc.account.service.OfflineJavaData;
import com.soulfiremc.account.service.OnlineJavaData;
import com.soulfiremc.server.account.MCAuthService;
import com.soulfiremc.server.account.SFBedrockMicrosoftAuthService;
import com.soulfiremc.server.account.SFEasyMCAuthService;
import com.soulfiremc.server.account.SFJavaMicrosoftAuthService;
import com.soulfiremc.server.account.SFOfflineAuthService;
import com.soulfiremc.server.account.SFTheAlteningAuthService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthType {
  MICROSOFT_JAVA("Microsoft Java", OnlineJavaData.class, new SFJavaMicrosoftAuthService()),
  MICROSOFT_BEDROCK("Microsoft Bedrock", BedrockData.class, new SFBedrockMicrosoftAuthService()),
  THE_ALTENING("The Altening", OnlineJavaData.class, new SFTheAlteningAuthService()),
  EASYMC("EasyMC", OnlineJavaData.class, new SFEasyMCAuthService()),
  OFFLINE("Offline", OfflineJavaData.class, new SFOfflineAuthService());

  private final String displayName;
  private final Class<? extends AccountData> accountDataClass;
  private final MCAuthService<?> authService;

  @Override
  public String toString() {
    return displayName;
  }
}
