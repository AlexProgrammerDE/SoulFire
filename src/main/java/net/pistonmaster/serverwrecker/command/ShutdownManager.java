/*
 * ServerWrecker
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
package net.pistonmaster.serverwrecker.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.ServerWreckerBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class ShutdownManager {
    public static final Logger LOGGER = LoggerFactory.getLogger(ShutdownManager.class);
    private final Runnable shutdownHook;
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    @Getter
    private boolean shutdown = false;

    /**
     * Shuts down the software if it is running.
     *
     * @param explicitExit whether the user explicitly shut down the software
     */
    public void shutdownSoftware(boolean explicitExit) {
        if (!shutdownInProgress.compareAndSet(false, true)) {
            return;
        }

        if (explicitExit) {
            LOGGER.info("Shutting down...");
        }

        shutdownHook.run();

        ServerWreckerBootstrap.PLUGIN_MANAGER.stopPlugins();
        ServerWreckerBootstrap.PLUGIN_MANAGER.unloadPlugins();

        shutdown = true;

        if (explicitExit) {
            System.exit(0);
        }
    }
}
