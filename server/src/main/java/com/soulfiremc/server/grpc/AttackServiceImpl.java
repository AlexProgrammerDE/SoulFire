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

import com.soulfiremc.grpc.generated.AttackServiceGrpc;
import com.soulfiremc.grpc.generated.AttackStartRequest;
import com.soulfiremc.grpc.generated.AttackStartResponse;
import com.soulfiremc.grpc.generated.AttackStateToggleRequest;
import com.soulfiremc.grpc.generated.AttackStateToggleResponse;
import com.soulfiremc.grpc.generated.AttackStopRequest;
import com.soulfiremc.grpc.generated.AttackStopResponse;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.settings.lib.SettingsHolder;
import io.grpc.stub.StreamObserver;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AttackServiceImpl extends AttackServiceGrpc.AttackServiceImplBase {
  private final SoulFireServer soulFireServer;

  @Override
  public void startAttack(
      AttackStartRequest request, StreamObserver<AttackStartResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().canAccessOrThrow(Resource.START_ATTACK);

    var settingsHolder = SettingsHolder.deserialize(request);

    var id = soulFireServer.startAttack(settingsHolder);
    responseObserver.onNext(AttackStartResponse.newBuilder().setId(id).build());
    responseObserver.onCompleted();
  }

  @Override
  public void toggleAttackState(
      AttackStateToggleRequest request,
      StreamObserver<AttackStateToggleResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().canAccessOrThrow(Resource.TOGGLE_ATTACK);

    soulFireServer.toggleAttackState(
        request.getId(),
        switch (request.getNewState()) {
          case PAUSE -> true;
          case RESUME, UNRECOGNIZED -> false;
        });
    responseObserver.onNext(AttackStateToggleResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void stopAttack(
      AttackStopRequest request, StreamObserver<AttackStopResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().canAccessOrThrow(Resource.STOP_ATTACK);

    soulFireServer.stopAttack(request.getId());
    responseObserver.onNext(AttackStopResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
