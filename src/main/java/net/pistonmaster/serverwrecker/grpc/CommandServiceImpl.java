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
import net.pistonmaster.serverwrecker.command.ServerCommandManager;
import net.pistonmaster.serverwrecker.grpc.generated.CommandCompletionRequest;
import net.pistonmaster.serverwrecker.grpc.generated.CommandCompletionResponse;
import net.pistonmaster.serverwrecker.grpc.generated.CommandHistoryRequest;
import net.pistonmaster.serverwrecker.grpc.generated.CommandHistoryResponse;
import net.pistonmaster.serverwrecker.grpc.generated.CommandRequest;
import net.pistonmaster.serverwrecker.grpc.generated.CommandResponse;
import net.pistonmaster.serverwrecker.grpc.generated.CommandServiceGrpc;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CommandServiceImpl extends CommandServiceGrpc.CommandServiceImplBase {
    private final ServerCommandManager serverCommandManager;

    @Override
    public void executeCommand(CommandRequest request, StreamObserver<CommandResponse> responseObserver) {
        var code = serverCommandManager.execute(request.getCommand());

        responseObserver.onNext(CommandResponse.newBuilder().setCode(code).build());
        responseObserver.onCompleted();
    }

    @Override
    public void tabCompleteCommand(CommandCompletionRequest request, StreamObserver<CommandCompletionResponse> responseObserver) {
        var suggestions = serverCommandManager.getCompletionSuggestions(request.getCommand());

        responseObserver.onNext(CommandCompletionResponse.newBuilder().addAllSuggestions(suggestions).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getCommandHistory(CommandHistoryRequest request, StreamObserver<CommandHistoryResponse> responseObserver) {
        var history = serverCommandManager.getCommandHistory();
        var builder = CommandHistoryResponse.newBuilder();
        for (var entry : history) {
            builder.addEntriesBuilder()
                    .setTimestamp(entry.getKey().getEpochSecond())
                    .setCommand(entry.getValue())
                    .build();
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
