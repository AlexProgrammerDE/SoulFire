package com.github.games647.lambdaattack;

import com.github.games647.lambdaattack.gui.MainGui;
import java.awt.GraphicsEnvironment;
import java.util.logging.Level;
import org.apache.commons.cli.ParseException;

public class Main {

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LambdaAttack.getLogger().log(Level.SEVERE, throwable.getMessage(), throwable);
        });

        if (GraphicsEnvironment.isHeadless() || args.length > 0) {
            runHeadless(args);
        } else {
            new MainGui(LambdaAttack.getInstance());
        }
    }

    private static void runHeadless(String[] args) {
        if (args.length == 0) {
            CommandLineParser.printHelp();
            return;
        }

        // parse the command line args
        CommandLineParser.ParseResult result;
        try {
            result = CommandLineParser.parse(args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            CommandLineParser.printHelp();
            return;
        }

        if (result.showHelp) {
            CommandLineParser.printHelp();
            return;
        }

        LambdaAttack.getInstance().start(result.options);
    }

}
