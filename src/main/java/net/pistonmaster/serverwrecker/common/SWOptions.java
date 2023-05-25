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
package net.pistonmaster.serverwrecker.common;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.pistonmaster.serverwrecker.auth.AuthService;

public record SWOptions(String host, int port,
                        int amount, int joinDelayMs, boolean waitEstablished, String botNameFormat,
                        ProtocolVersion protocolVersion, boolean autoRegister,
                        boolean debug, ProxyType proxyType, int accountsPerProxy,
                        int readTimeout, int writeTimeout, int connectTimeout,
                        String registerCommand, String loginCommand, String captchaCommand,
                        String passwordFormat, boolean autoReconnect, boolean autoRespawn, AuthService authService) {
}
