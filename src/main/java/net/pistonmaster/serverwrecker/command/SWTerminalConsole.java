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
package net.pistonmaster.serverwrecker.command;

import lombok.RequiredArgsConstructor;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import net.pistonmaster.serverwrecker.grpc.RPCClient;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;

import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public class SWTerminalConsole extends SimpleTerminalConsole {
    private static final Logger logger = LogManager.getLogger("ServerWrecker");
    private final ShutdownManager shutdownManager;
    private final ClientCommandManager clientCommandManager;

    /**
     * Sets up {@code System.out} and {@code System.err} to redirect to log4j.
     */
    public static void setupStreams() {
        System.setOut(IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintStream());
        System.setErr(IoBuilder.forLogger(logger).setLevel(Level.ERROR).buildPrintStream());
    }

    public static void setupTerminalConsole(ExecutorService threadPool, ShutdownManager shutdownManager, RPCClient rpcClient) {
        SWTerminalConsole.setupStreams();
        threadPool.execute(new SWTerminalConsole(shutdownManager, new ClientCommandManager(rpcClient))::start);
    }

    @Override
    protected boolean isRunning() {
        return !shutdownManager.isShutdown();
    }

    @Override
    protected void runCommand(String command) {
        clientCommandManager.execute(command);
    }

    @Override
    protected void shutdown() {
        shutdownManager.shutdown(true);
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        var history = new DefaultHistory();
        for (var command : clientCommandManager.getCommandHistory()) {
            history.add(command.getKey(), command.getValue());
        }

        return super.buildReader(builder
                .appName("ServerWrecker")
                .completer((reader, parsedLine, list) -> {
                    for (String suggestion : clientCommandManager.getCompletionSuggestions(parsedLine.line())) {
                        list.add(new Candidate(suggestion));
                    }
                })
                .history(history)
        );
    }
}
