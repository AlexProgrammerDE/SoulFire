/*
 * ServerWrecker
 *
 * Copyright (C) 2021 ServerWrecker
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
package net.pistonmaster.serverwrecker;

import net.pistonmaster.serverwrecker.common.GameVersion;
import net.pistonmaster.serverwrecker.common.ProxyType;
import org.apache.commons.cli.*;

import java.util.Arrays;
import java.util.stream.Collectors;

public class CommandLineParser {

    private final Options options;

    private final StringOption hostOption;
    private final IntOption portOption;
    private final StringOption versionOption;
    private final StringOption proxyOption;

    private final IntOption amountOption;
    private final IntOption joinDelayOption;
    private final StringOption nameFormatOption;

    private final IntOption accountsPerProxyOption;

    private final Option autoRegisterOption;
    private final Option helpOption;
    private final Option debugOption;

    private CommandLine cmd;

    private CommandLineParser() {
        options = new Options();

        hostOption = new StringOption(new Option("h", "host", true, "The hostname to connect to. Defaults to 127.0.0.1"), "127.0.0.1");
        options.addOption(hostOption.option);

        portOption = new IntOption(new Option("p", "port", true, "The port to connect to. Defaults to 25565"), 25565);
        options.addOption(portOption.option);

        amountOption = new IntOption(new Option("c", "count", true, "The amount of bots to connect to the server. Defaults to 20"), 20);
        options.addOption(amountOption.option);

        joinDelayOption = new IntOption(new Option("d", "delay", true, "The delay between bot spawns, in milliseconds. Defaults to 1000"), 1000);
        options.addOption(joinDelayOption.option);

        nameFormatOption = new StringOption(new Option("n", "name", true, "The format for bot names. Requires exactly one integer placeholder '%d'. Defaults to 'Bot-%d'"), "Bot-%d");
        options.addOption(nameFormatOption.option);

        GameVersion latestVer = GameVersion.getNewest();
        versionOption = new StringOption(new Option("v", "version", true, "The Minecraft version of the server to connect to. Defaults to " + latestVer.getVersion()), latestVer.getVersion());
        options.addOption(versionOption.option);

        autoRegisterOption = new Option("r", "register", false, "Makes Bots run the /register and /login command after joining with username and password being " + ServerWrecker.PROJECT_NAME);
        options.addOption(autoRegisterOption);

        helpOption = new Option(null, "help", false, "Displays this help page");
        options.addOption(helpOption);

        debugOption = new Option(null, "debug", false, "Logs additional information useful for debugging");
        options.addOption(debugOption);

        proxyOption = new StringOption(new Option(null, "proxytype", true, "The proxies type. Defaults to " + ProxyType.SOCKS5.name()), ProxyType.SOCKS5.name());
        options.addOption(proxyOption.option);

        accountsPerProxyOption = new IntOption(new Option(null, "accountsperproxy", true, "How many accounts can be on a single proxy. Defaults to -1"), -1);
        options.addOption(accountsPerProxyOption.option);
    }

    static ParseResult parse(String[] args) throws ParseException {
        CommandLineParser cli = new CommandLineParser();
        cli.doParse(args);

        return new ParseResult(
                cli.shouldPrintHelp(),
                new net.pistonmaster.serverwrecker.common.Options(
                        cli.getHostname(),
                        cli.getPort(),
                        cli.getAmount(),
                        cli.getJoinDelayMs(),
                        cli.getBotNameFormat(),
                        cli.getGameVersion(),
                        cli.getAutoRegister(),
                        cli.getDebug(),
                        cli.getProxyType(),
                        cli.getAccountsPerProxy()));
    }

    static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(ServerWrecker.PROJECT_NAME, new CommandLineParser().options);
    }

    private void doParse(String[] args) throws ParseException {
        org.apache.commons.cli.CommandLineParser parser = new DefaultParser();
        cmd = parser.parse(options, args);
    }

    private boolean shouldPrintHelp() {
        return cmd.hasOption(helpOption.getLongOpt());
    }

    private String getHostname() throws ParseException {
        return hostOption.get(cmd);
    }

    private int getPort() throws ParseException {
        return portOption.get(cmd);
    }

    private int getAmount() throws ParseException {
        return amountOption.get(cmd);
    }

    private int getJoinDelayMs() throws ParseException {
        return joinDelayOption.get(cmd);
    }

    private String getBotNameFormat() throws ParseException {
        return nameFormatOption.get(cmd);
    }

    private GameVersion getGameVersion() throws ParseException {
        String value = versionOption.get(cmd);
        GameVersion gameVersion = GameVersion.findByName(value);

        if (gameVersion == null) {
            throw new ParseException(String.format("Unsupported Minecraft version: %s.%nSupported versions are: %s", value,
                    Arrays.stream(GameVersion.values())
                            .map(GameVersion::getVersion)
                            .collect(Collectors.joining(", "))));
        }

        return gameVersion;
    }

    private boolean getAutoRegister() {
        return cmd.hasOption(autoRegisterOption.getOpt());
    }

    private boolean getDebug() {
        return cmd.hasOption(debugOption.getOpt());
    }

    private ProxyType getProxyType() throws ParseException {
        String value = versionOption.get(cmd);
        ProxyType gameVersion = ProxyType.findByName(value);

        if (gameVersion == null) {
            throw new ParseException(String.format("Unsupported Minecraft version: %s.%nSupported versions are: %s", value,
                    Arrays.stream(ProxyType.values())
                            .map(ProxyType::name)
                            .collect(Collectors.joining(", "))));
        }

        return gameVersion;
    }

    private int getAccountsPerProxy() throws ParseException {
        return accountsPerProxyOption.get(cmd);
    }

    static class ParseResult {
        final boolean showHelp;
        final net.pistonmaster.serverwrecker.common.Options options;

        ParseResult(boolean showHelp, net.pistonmaster.serverwrecker.common.Options options) {
            this.showHelp = showHelp;
            this.options = options;
        }
    }

    private static class TypedOption<T> {
        final Option option;
        final T defaultValue;

        TypedOption(Option option, T defaultValue, Class<T> typeClass) {
            this.option = option;
            this.option.setType(typeClass);

            this.defaultValue = defaultValue;
        }

        @SuppressWarnings("unchecked")
        T convertParsedToType(Object parsed) {
            return (T) parsed;
        }

        T get(CommandLine cmd) throws ParseException {
            T value = convertParsedToType(cmd.getParsedOptionValue(option.getOpt()));
            return value != null ? value : defaultValue;
        }
    }

    private static class StringOption extends TypedOption<String> {
        StringOption(Option option, String defaultValue) {
            super(option, defaultValue, String.class);
        }
    }

    private static class IntOption extends TypedOption<Integer> {
        IntOption(Option option, int defaultValue) {
            super(option, defaultValue, Integer.class);
        }

        @Override
        Integer convertParsedToType(Object parsed) {
            Number number = (Number) parsed;
            if (number == null) {
                return null;
            }

            return number.intValue();
        }
    }

}
