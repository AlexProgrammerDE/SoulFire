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
package net.pistonmaster.soulfire.server.api.event.attack;

import net.pistonmaster.soulfire.server.AttackManager;
import net.pistonmaster.soulfire.server.api.event.SoulFireGlobalEvent;

/**
 * This event is called right after an AttackManager is created and before any attack is scheduled.
 * This is intentionally in a global scope for any listener to subscribe to this AttackManager.
 *
 * @param attackManager The attack manager instance.
 */
public record AttackInitEvent(AttackManager attackManager) implements SoulFireGlobalEvent {
}