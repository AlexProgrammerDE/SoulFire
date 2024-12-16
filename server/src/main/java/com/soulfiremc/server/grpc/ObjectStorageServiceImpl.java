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

import com.google.protobuf.ByteString;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.SFHelpers;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.nio.file.Files;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ObjectStorageServiceImpl extends ObjectStorageServiceGrpc.ObjectStorageServiceImplBase {
  private final SoulFireServer soulFireServer;

  @Override
  public void upload(ObjectStorageUploadRequest request, StreamObserver<ObjectStorageUploadResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPLOAD_OBJECT_STORAGE, instanceId));
    var optionalInstance = soulFireServer.getInstance(instanceId);
    if (optionalInstance.isEmpty()) {
      throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
    }

    var instance = optionalInstance.get();

    try {
      var filePath = instance.getObjectStoragePath().resolve(SFHelpers.sanitizeFileName(request.getFileName()));
      Files.write(filePath, request.getData().toByteArray());

      responseObserver.onNext(ObjectStorageUploadResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error uploading object storage", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void download(ObjectStorageDownloadRequest request, StreamObserver<ObjectStorageDownloadResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.DOWNLOAD_OBJECT_STORAGE, instanceId));
    var optionalInstance = soulFireServer.getInstance(instanceId);
    if (optionalInstance.isEmpty()) {
      throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
    }

    var instance = optionalInstance.get();

    try {
      var filePath = instance.getObjectStoragePath().resolve(SFHelpers.sanitizeFileName(request.getFileName()));
      var data = Files.readAllBytes(filePath);

      responseObserver.onNext(ObjectStorageDownloadResponse.newBuilder().setData(ByteString.copyFrom(data)).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error downloading object storage", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void delete(ObjectStorageDeleteRequest request, StreamObserver<ObjectStorageDeleteResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.DELETE_OBJECT_STORAGE, instanceId));
    var optionalInstance = soulFireServer.getInstance(instanceId);
    if (optionalInstance.isEmpty()) {
      throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
    }

    var instance = optionalInstance.get();

    try {
      var filePath = instance.getObjectStoragePath().resolve(SFHelpers.sanitizeFileName(request.getFileName()));
      Files.delete(filePath);

      responseObserver.onNext(ObjectStorageDeleteResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error deleting object storage", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void list(ObjectStorageListRequest request, StreamObserver<ObjectStorageListResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.LIST_OBJECT_STORAGE, instanceId));
    var optionalInstance = soulFireServer.getInstance(instanceId);
    if (optionalInstance.isEmpty()) {
      throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
    }

    var instance = optionalInstance.get();

    try {
      responseObserver.onNext(ObjectStorageListResponse.newBuilder().addAllFileNames(
        Files.list(instance.getObjectStoragePath())
          .map(path -> path.getFileName().toString())
          .toList()
      ).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error listing object storage", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
