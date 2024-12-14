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

import java.util.UUID;

public sealed interface Permission permits Permission.Global, Permission.Instance {
  static Global global(String id, String description) {
    return new Global(id, description);
  }

  static Instance instance(String id, String description) {
    return new Instance(id, description);
  }

  String id();

  String description();

  sealed interface Context permits GlobalContext, InstanceContext {
  }

  record Global(String id, String description) implements Permission {
    public GlobalContext context() {
      return new GlobalContext(id);
    }
  }

  record Instance(String id, String description) implements Permission {
    public InstanceContext context(UUID instanceId) {
      return new InstanceContext(id, instanceId);
    }
  }

  record GlobalContext(String id) implements Context {
  }

  record InstanceContext(String id, UUID instanceId) implements Context {
  }
}
