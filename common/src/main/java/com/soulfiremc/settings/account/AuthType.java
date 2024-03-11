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
package com.soulfiremc.settings.account;

import com.soulfiremc.settings.account.service.AccountData;
import com.soulfiremc.settings.account.service.BedrockData;
import com.soulfiremc.settings.account.service.OfflineJavaData;
import com.soulfiremc.settings.account.service.OnlineJavaData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthType {
  MICROSOFT_JAVA("Microsoft Java", OnlineJavaData.class),
  MICROSOFT_BEDROCK("Microsoft Bedrock", BedrockData.class),
  THE_ALTENING("The Altening", OnlineJavaData.class),
  EASY_MC("EasyMC", OnlineJavaData.class),
  OFFLINE("Offline", OfflineJavaData.class);

  private final String displayName;
  private final Class<? extends AccountData> accountDataClass;

  @Override
  public String toString() {
    return displayName;
  }
}
