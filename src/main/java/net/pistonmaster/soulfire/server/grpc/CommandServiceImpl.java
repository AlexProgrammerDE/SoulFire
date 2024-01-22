/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.soulfire.server.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.soulfire.grpc.generated.*;
import net.pistonmaster.soulfire.server.ServerCommandManager;

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
