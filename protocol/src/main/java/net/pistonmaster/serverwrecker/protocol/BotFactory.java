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
import net.pistonmaster.serverwrecker.version.v1_12.Bot1_12;
import net.pistonmaster.serverwrecker.version.v1_17.Bot1_17;
import net.pistonmaster.serverwrecker.version.v1_18.Bot1_18;
import net.pistonmaster.serverwrecker.version.v1_19.Bot1_19;
import net.pistonmaster.serverwrecker.version.v1_7.Bot1_7;
import net.pistonmaster.serverwrecker.version.v1_8.Bot1_8;
import net.pistonmaster.serverwrecker.version.v1_9.Bot1_9;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class BotFactory {
    public AbstractBot createBot(Options options, IPacketWrapper account, ServiceServer serviceServer) {
        return createBot(options, account, null, serviceServer, null, null, null);
    }

    public AbstractBot createBot(Options options, IPacketWrapper account, InetSocketAddress address, ServiceServer serviceServer, ProxyType proxyType, String username, String password) {
        Logger botLogger = LoggerFactory.getLogger(account.getProfileName());

        return switch (options.gameVersion()) {
            case VERSION_1_7 ->
                    new Bot1_7(options, account, address, serviceServer, proxyType, username, password, botLogger);
            case VERSION_1_8 ->
                    new Bot1_8(options, account, address, serviceServer, proxyType, username, password, botLogger);
            case VERSION_1_9 ->
                    new Bot1_9(options, account, address, serviceServer, proxyType, username, password, botLogger);
            case VERSION_1_12 ->
                    new Bot1_12(options, account, address, serviceServer, proxyType, username, password, botLogger);
            case VERSION_1_17 ->
                    new Bot1_17(options, account, address, serviceServer, proxyType, username, password, botLogger);
            case VERSION_1_18 ->
                    new Bot1_18(options, account, address, serviceServer, proxyType, username, password, botLogger);
            case VERSION_1_19 ->
                    new Bot1_19(options, account, address, serviceServer, proxyType, username, password, botLogger);
        };
    }
}
