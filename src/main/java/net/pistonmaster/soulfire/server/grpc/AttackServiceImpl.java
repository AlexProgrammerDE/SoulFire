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
import net.pistonmaster.soulfire.client.settings.SettingsManager;
import net.pistonmaster.soulfire.grpc.generated.*;
import net.pistonmaster.soulfire.server.SoulFireServer;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AttackServiceImpl extends AttackServiceGrpc.AttackServiceImplBase {
    private final SoulFireServer soulFireServer;

    @Override
    public void startAttack(AttackStartRequest request, StreamObserver<AttackStartResponse> responseObserver) {
        var settingsHolder = SettingsManager.createSettingsHolder(request.getSettings(), null);

        var id = soulFireServer.startAttack(settingsHolder);
        responseObserver.onNext(AttackStartResponse.newBuilder().setId(id).build());
        responseObserver.onCompleted();
    }

    @Override
    public void toggleAttackState(AttackStateToggleRequest request, StreamObserver<AttackStateToggleResponse> responseObserver) {
        soulFireServer.toggleAttackState(request.getId(), switch (request.getNewState()) {
            case PAUSE -> true;
            case RESUME, UNRECOGNIZED -> false;
        });
        responseObserver.onNext(AttackStateToggleResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void stopAttack(AttackStopRequest request, StreamObserver<AttackStopResponse> responseObserver) {
        soulFireServer.stopAttack(request.getId());
        responseObserver.onNext(AttackStopResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
