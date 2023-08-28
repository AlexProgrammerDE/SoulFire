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
package net.pistonmaster.serverwrecker.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.kyori.event.EventSubscriber;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.system.SystemLogEvent;
import net.pistonmaster.serverwrecker.grpc.generated.command.*;
import net.pistonmaster.serverwrecker.grpc.generated.logs.LogRequest;
import net.pistonmaster.serverwrecker.grpc.generated.logs.LogResponse;
import net.pistonmaster.serverwrecker.grpc.generated.logs.LogsServiceGrpc;
import net.pistonmaster.serverwrecker.logging.CommandManager;
import net.pistonmaster.serverwrecker.logging.SWTerminalConsole;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CommandServiceImpl extends CommandServiceGrpc.CommandServiceImplBase {
    private final CommandManager commandManager;

    @Override
    public void executeCommand(CommandRequest request, StreamObserver<CommandResponse> responseObserver) {
        int code = commandManager.execute(request.getCommand());

        responseObserver.onNext(CommandResponse.newBuilder().setCodeValue(code).build());
        responseObserver.onCompleted();
    }

    @Override
    public void tabCompleteCommand(CommandCompletionRequest request, StreamObserver<CommandCompletionResponse> responseObserver) {
        List<String> suggestions = commandManager.getCompletionSuggestions(request.getCommand());

        responseObserver.onNext(CommandCompletionResponse.newBuilder().addAllSuggestions(suggestions).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getCommandHistory(CommandHistoryRequest request, StreamObserver<CommandHistoryResponse> responseObserver) {
        List<String> history = commandManager.getCommandHistory();

        responseObserver.onNext(CommandHistoryResponse.newBuilder().addAllCommand(history).build());
        responseObserver.onCompleted();
    }
}
