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
import net.pistonmaster.soulfire.account.service.AccountData;
import net.pistonmaster.soulfire.account.service.BedrockData;
import net.pistonmaster.soulfire.account.service.MCAuthService;
import net.pistonmaster.soulfire.account.service.OfflineJavaData;
import net.pistonmaster.soulfire.account.service.OnlineJavaData;
import net.pistonmaster.soulfire.account.service.SFBedrockMicrosoftAuthService;
import net.pistonmaster.soulfire.account.service.SFEasyMCAuthService;
import net.pistonmaster.soulfire.account.service.SFJavaMicrosoftAuthService;
import net.pistonmaster.soulfire.account.service.SFOfflineAuthService;
import net.pistonmaster.soulfire.account.service.SFTheAlteningAuthService;

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
