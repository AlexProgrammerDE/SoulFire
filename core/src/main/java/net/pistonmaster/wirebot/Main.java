package net.pistonmaster.wirebot;

import net.pistonmaster.wirebot.gui.MainFrame;
import org.apache.commons.cli.ParseException;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;

import java.awt.*;
import java.io.File;
import java.util.logging.Level;

public class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            WireBot.getLogger().log(Level.SEVERE, throwable.getMessage(), throwable);
        });

        if (GraphicsEnvironment.isHeadless() || args.length > 0) {
            runHeadless(args);
        } else {
            new MainFrame(WireBot.getInstance());
        }

        initPlugins();
    }

    private static void initPlugins() {
        // create the plugin manager
        PluginManager pluginManager = new JarPluginManager(); // or "new ZipPluginManager() / new DefaultPluginManager()"

        // start and load all plugins of application
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
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

        WireBot.getInstance().start(result.options);
    }
}
