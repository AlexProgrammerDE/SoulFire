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
package net.pistonmaster.serverwrecker.logging;

import lombok.RequiredArgsConstructor;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import net.pistonmaster.serverwrecker.ServerWrecker;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import com.google.inject.Inject;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SWTerminalConsole extends SimpleTerminalConsole {
    private static final Logger logger = LogManager.getLogger("ServerWrecker");
    private final ServerWrecker serverWrecker;
    private final CommandManager commandManager;

    /**
     * Sets up {@code System.out} and {@code System.err} to redirect to log4j.
     */
    public void setupStreams() {
        System.setOut(IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintStream());
        System.setErr(IoBuilder.forLogger(logger).setLevel(Level.ERROR).buildPrintStream());
    }

    @Override
    protected boolean isRunning() {
        return !serverWrecker.isShutdown();
    }

    @Override
    protected void runCommand(String command) {
        commandManager.execute(command);
    }

    @Override
    protected void shutdown() {
        serverWrecker.shutdown(true);
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        return super.buildReader(builder
                .appName("ServerWrecker")
                .completer((reader, parsedLine, list) -> {
                    try {
                        List<String> offers = commandManager.getCompletionSuggestions(parsedLine.line()); // Console doesn't get harmed much by this...
                        for (String offer : offers) {
                            list.add(new Candidate(offer));
                        }
                    } catch (Exception e) {
                        logger.error("An error occurred while trying to perform tab completion.", e);
                    }
                })
        );
    }
}
