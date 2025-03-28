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
package com.soulfiremc.server.grpc;

import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class ScriptServiceImpl extends ScriptServiceGrpc.ScriptServiceImplBase {
  private final SoulFireServer soulFireServer;

  @Override
  public void createScript(CreateScriptRequest request, StreamObserver<CreateScriptResponse> responseObserver) {
    super.createScript(request, responseObserver);
  }

  @Override
  public void deleteScript(DeleteScriptRequest request, StreamObserver<DeleteScriptResponse> responseObserver) {
    super.deleteScript(request, responseObserver);
  }

  @Override
  public void restartScript(RestartScriptRequest request, StreamObserver<RestartScriptResponse> responseObserver) {
    super.restartScript(request, responseObserver);
  }

  @Override
  public void updateScript(UpdateScriptRequest request, StreamObserver<UpdateScriptResponse> responseObserver) {
    super.updateScript(request, responseObserver);
  }
}
