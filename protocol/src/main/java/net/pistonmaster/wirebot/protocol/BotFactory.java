package net.pistonmaster.wirebot.protocol;

import net.pistonmaster.wirebot.common.*;
import net.pistonmaster.wirebot.version.v1_10.Bot1_10;
import net.pistonmaster.wirebot.version.v1_11.Bot1_11;
import net.pistonmaster.wirebot.version.v1_12.Bot1_12;
import net.pistonmaster.wirebot.version.v1_13.Bot1_13;
import net.pistonmaster.wirebot.version.v1_14.Bot1_14;
import net.pistonmaster.wirebot.version.v1_15.Bot1_15;
import net.pistonmaster.wirebot.version.v1_16.Bot1_16;
import net.pistonmaster.wirebot.version.v1_17.Bot1_17;
import net.pistonmaster.wirebot.version.v1_8.Bot1_8;
import net.pistonmaster.wirebot.version.v1_9.Bot1_9;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class BotFactory {
    public AbstractBot createBot(Options options, IPacketWrapper account, Logger log, ServiceServer serviceServer) {
        return createBot(options, account, null, log, serviceServer, null);
    }

    public AbstractBot createBot(Options options, IPacketWrapper account, InetSocketAddress address, Logger log, ServiceServer serviceServer, ProxyType proxyType) {
        Logger botLogger = Logger.getLogger(account.getProfileName());
        botLogger.setParent(log);

        return switch (options.gameVersion) {
            case VERSION_1_8 -> new Bot1_8(options, account, address, log, serviceServer, proxyType);
            case VERSION_1_9 -> new Bot1_9(options, account, address, log, serviceServer, proxyType);
            case VERSION_1_10 -> new Bot1_10(options, account, address, log, serviceServer, proxyType);
            case VERSION_1_11 -> new Bot1_11(options, account, address, log, serviceServer, proxyType);
            case VERSION_1_12 -> new Bot1_12(options, account, address, log, serviceServer, proxyType);
            case VERSION_1_13 -> new Bot1_13(options, account, address, log, serviceServer, proxyType);
            case VERSION_1_14 -> new Bot1_14(options, account, address, log, serviceServer, proxyType);
            case VERSION_1_15 -> new Bot1_15(options, account, address, log, serviceServer, proxyType);
            case VERSION_1_16 -> new Bot1_16(options, account, address, log, serviceServer, proxyType);
            case VERSION_1_17 -> new Bot1_17(options, account, address, log, serviceServer, proxyType);
        };
    }
}
