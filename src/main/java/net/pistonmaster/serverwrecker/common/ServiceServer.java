/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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
package net.pistonmaster.serverwrecker.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ServiceServer {
    // , URI.create("https://login.microsoftonline.com/"), URI.create("https://login.microsoftonline.com/")
    MOJANG("Mojang"),
    MICROSOFT("Microsoft", List.of("clientId")),
    OFFLINE("Offline");
    // THE_ALTENING("The Altening (1.16+ only)", URI.create("https://authserver.thealtening.com/"), URI.create("https://sessionserver.thealtening.com/"));

    private final String name;
    private final List<String> configKeys;

    ServiceServer(String name) {
        this(name, List.of());
    }

    @Override
    public String toString() {
        return name;
    }
}
