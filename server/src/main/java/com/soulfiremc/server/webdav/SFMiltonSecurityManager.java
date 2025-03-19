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
package com.soulfiremc.server.webdav;

import com.soulfiremc.server.SoulFireServer;
import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.SecurityManager;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.Resource;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

@RequiredArgsConstructor
public class SFMiltonSecurityManager implements SecurityManager {
  private final SoulFireServer soulFireServer;

  @Override
  public @Nullable Object authenticate(DigestResponse digestRequest) {
    return null;
  }

  @Override
  public Object authenticate(String user, String password) {
    return user;
  }

  @Override
  public boolean authorise(Request request, Request.Method method, Auth auth, Resource resource) {
    System.out.println("AUTH " + method + " " + resource.getName());
    return true;
  }

  @Override
  public String getRealm(String host) {
    return "soulfire";
  }

  @Override
  public boolean isDigestAllowed() {
    return false;
  }
}
