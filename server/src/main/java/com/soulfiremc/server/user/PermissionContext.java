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
package com.soulfiremc.server.user;

import com.soulfiremc.grpc.generated.GlobalPermission;
import com.soulfiremc.grpc.generated.InstancePermission;

import java.util.UUID;

public sealed interface PermissionContext permits PermissionContext.GlobalContext, PermissionContext.InstanceContext {
  static GlobalContext global(GlobalPermission globalPermission) {
    return new GlobalContext(globalPermission);
  }

  static InstanceContext instance(InstancePermission instancePermission, UUID instanceId) {
    return new InstanceContext(instancePermission, instanceId);
  }

  record GlobalContext(GlobalPermission globalPermission) implements PermissionContext {
  }

  record InstanceContext(InstancePermission instancePermission, UUID instanceId) implements PermissionContext {
  }
}
