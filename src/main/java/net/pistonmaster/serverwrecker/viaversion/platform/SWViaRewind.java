/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.viaversion.platform;

import com.viaversion.viarewind.api.ViaRewindPlatform;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.viaversion.JLoggerToSLF4J;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class SWViaRewind implements ViaRewindPlatform {
    private final JLoggerToSLF4J logger = new JLoggerToSLF4J(LoggerFactory.getLogger("ViaRewind"));
    private final Path dataFolder;

    @Override
    public Logger getLogger() {
        return logger;
    }

    public void init() {
        init(dataFolder.resolve("config.yml").toFile());
    }
}
