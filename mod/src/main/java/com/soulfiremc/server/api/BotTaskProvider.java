/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.api;

import com.google.protobuf.Message;
import com.soulfiremc.server.bot.ControlResource;
import com.soulfiremc.server.task.BotTaskContext;

import java.util.Set;

/// Creates a typed server-side task from a protobuf request.
public interface BotTaskProvider<I extends Message> {
  I inputPrototype();

  default String summary(I input) {
    return input.getDescriptorForType().getName();
  }

  default Set<ControlResource> resources(I input) {
    return ControlResource.all();
  }

  /// Prepares one durable task. The factory is reused across its connections.
  /// Retain only detached recovery data here, never a bot or an execution.
  default ExecutionFactory prepare(I input) {
    return context -> start(context, input);
  }

  BotTaskExecution start(BotTaskContext context, I input) throws Exception;

  @FunctionalInterface
  interface ExecutionFactory {
    BotTaskExecution start(BotTaskContext context) throws Exception;
  }
}
