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

import javax.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ObjectStorageServiceImpl extends ObjectStorageServiceGrpc.ObjectStorageServiceImplBase {
  private final SoulFireServer soulFireServer;

  @Override
  public void upload(ObjectStorageUploadRequest request, StreamObserver<ObjectStorageUploadResponse> responseObserver) {
    super.upload(request, responseObserver);
  }

  @Override
  public void download(ObjectStorageDownloadRequest request, StreamObserver<ObjectStorageDownloadResponse> responseObserver) {
    super.download(request, responseObserver);
  }

  @Override
  public void delete(ObjectStorageDeleteRequest request, StreamObserver<ObjectStorageDeleteResponse> responseObserver) {
    super.delete(request, responseObserver);
  }

  @Override
  public void list(ObjectStorageListRequest request, StreamObserver<ObjectStorageListResponse> responseObserver) {
    super.list(request, responseObserver);
  }
}
