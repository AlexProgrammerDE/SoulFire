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
package net.pistonmaster.serverwrecker.protocol;

import net.pistonmaster.serverwrecker.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class BotFactory {
    public Bot createBot(SWOptions options, ProtocolWrapper account, ServiceServer serviceServer) {
        return createBot(options, account, null, serviceServer, null, null, null);
    }

    public Bot createBot(SWOptions options, ProtocolWrapper account, InetSocketAddress address, ServiceServer serviceServer, ProxyType proxyType, String username, String password) {
        Logger botLogger = LoggerFactory.getLogger(account.getProfileName());

        ProxyBotData proxyBotData = address == null ? null : ProxyBotData.of(username, password, address, proxyType);
        return new Bot(options, botLogger, account, serviceServer, proxyBotData);
    }
}
