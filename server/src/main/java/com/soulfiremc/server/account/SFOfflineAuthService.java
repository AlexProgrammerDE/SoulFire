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
package com.soulfiremc.server.account;

import com.soulfiremc.settings.account.AuthType;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.account.service.OfflineJavaData;
import com.soulfiremc.settings.proxy.SFProxy;

public final class SFOfflineAuthService
  implements MCAuthService<SFOfflineAuthService.OfflineAuthData> {
  public static MinecraftAccount createAccount(String username) {
    return new MinecraftAccount(
      AuthType.OFFLINE,
      OfflineJavaData.getOfflineUUID(username),
      username,
      new OfflineJavaData());
  }

  @Override
  public MinecraftAccount login(OfflineAuthData data, SFProxy proxyData) {
    return createAccount(data.username());
  }

  @Override
  public OfflineAuthData createData(String data) {
    return new OfflineAuthData(data);
  }

  public record OfflineAuthData(String username) {}
}
