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
package net.pistonmaster.serverwrecker.cli;

import lombok.Getter;
import net.pistonmaster.serverwrecker.command.ShutdownManager;
import net.pistonmaster.serverwrecker.grpc.RPCClient;
import net.pistonmaster.serverwrecker.settings.lib.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class CLIManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CLIManager.class);
    private final RPCClient rpcClient;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final ShutdownManager shutdownManager = new ShutdownManager(this::shutdownHook);
    private final SettingsManager settingsManager = new SettingsManager();

    public CLIManager(RPCClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public void initCLI(String[] args) {
        var serverWreckerCommand = new SWCommandDefinition(this);
        var commandLine = new CommandLine(serverWreckerCommand);
        serverWreckerCommand.setCommandLine(commandLine);
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        commandLine.setUsageHelpAutoWidth(true);
        commandLine.setUsageHelpLongOptionsMaxWidth(30);
        commandLine.setExecutionExceptionHandler((ex, cmdLine, parseResult) -> {
            LOGGER.error("Exception while executing command", ex);
            return 1;
        });

        commandLine.execute(args);
    }

    private void shutdownHook() {
        threadPool.shutdown();
    }

    public void shutdown() {
        shutdownManager.shutdown(true);
    }
}
