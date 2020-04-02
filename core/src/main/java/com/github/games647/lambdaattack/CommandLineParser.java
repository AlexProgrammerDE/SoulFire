package com.github.games647.lambdaattack;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommandLineParser {

    private final Options options;

    private final StringOption hostOption;
    private final IntOption portOption;
    private final StringOption versionOption;

    private final IntOption amountOption;
    private final IntOption joinDelayOption;
    private final StringOption nameFormatOption;

    private final Option autoRegisterOption;
    private final Option helpOption;

    private CommandLine cmd;

    static class ParseResult {
        final boolean showHelp;
        final com.github.games647.lambdaattack.Options options;

        ParseResult(boolean showHelp, com.github.games647.lambdaattack.Options options) {
            this.showHelp = showHelp;
            this.options = options;
        }
    }

    static ParseResult parse(String[] args) throws ParseException {
        CommandLineParser cli = new CommandLineParser();
        cli.doParse(args);

        return new ParseResult(
                cli.shouldPrintHelp(),
                new com.github.games647.lambdaattack.Options(
                        cli.getHostname(),
                        cli.getPort(),
                        cli.getAmount(),
                        cli.getJoinDelayMs(),
                        cli.getBotNameFormat(),
                        cli.getGameVersion(),
                        cli.getAutoRegister()));
    }

    static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(LambdaAttack.PROJECT_NAME, new CommandLineParser().options);
    }

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

        versionOption = new StringOption(new Option("v", "version", true, "The Minecraft version of the server to connect to. Defaults to 1.15.2"), GameVersion.VERSION_1_15.getVersion());
        options.addOption(versionOption.option);

        autoRegisterOption = new Option("r", "register", false, "Makes Bots run the /register and /login command after joining with username and password being " + LambdaAttack.PROJECT_NAME);
        options.addOption(autoRegisterOption);

        helpOption = new Option(null, "help", false, "Displays this help page");
        options.addOption(helpOption);
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

    private static class TypedOption<T> {
        final Option option;
        final T defaultValue;

        TypedOption(Option option, T defaultValue, Class typeClass) {
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
            super(option, defaultValue, Number.class);
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
