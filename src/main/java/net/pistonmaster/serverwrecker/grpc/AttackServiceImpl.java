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
import net.pistonmaster.serverwrecker.client.settings.SettingsManager;
import net.pistonmaster.serverwrecker.grpc.generated.AttackServiceGrpc;
import net.pistonmaster.serverwrecker.grpc.generated.AttackStartRequest;
import net.pistonmaster.serverwrecker.grpc.generated.AttackStartResponse;
import net.pistonmaster.serverwrecker.grpc.generated.AttackStateToggleRequest;
import net.pistonmaster.serverwrecker.grpc.generated.AttackStateToggleResponse;
import net.pistonmaster.serverwrecker.grpc.generated.AttackStopRequest;
import net.pistonmaster.serverwrecker.grpc.generated.AttackStopResponse;
import net.pistonmaster.serverwrecker.server.ServerWreckerServer;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AttackServiceImpl extends AttackServiceGrpc.AttackServiceImplBase {
    private final ServerWreckerServer serverWreckerServer;

    @Override
    public void startAttack(AttackStartRequest request, StreamObserver<AttackStartResponse> responseObserver) {
        var settingsHolder = SettingsManager.createSettingsHolder(request.getSettings(), null);

        var id = serverWreckerServer.startAttack(settingsHolder);
        responseObserver.onNext(AttackStartResponse.newBuilder().setId(id).build());
        responseObserver.onCompleted();
    }

    @Override
    public void toggleAttackState(AttackStateToggleRequest request, StreamObserver<AttackStateToggleResponse> responseObserver) {
        serverWreckerServer.toggleAttackState(request.getId(), switch (request.getNewState()) {
            case PAUSE -> true;
            case RESUME, UNRECOGNIZED -> false;
        });
        responseObserver.onNext(AttackStateToggleResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void stopAttack(AttackStopRequest request, StreamObserver<AttackStopResponse> responseObserver) {
        serverWreckerServer.stopAttack(request.getId());
        responseObserver.onNext(AttackStopResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
