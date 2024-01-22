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
package net.pistonmaster.soulfire.server.viaversion;

import com.viaversion.viaversion.api.platform.PlatformTask;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Future;

public record FutureTaskId(Future<?> object) implements PlatformTask<Future<?>> {
    @Deprecated
    @Override
    public @Nullable Future<?> getObject() {
        return object;
    }

    @Override
    public void cancel() {
        object.cancel(false);
    }
}
