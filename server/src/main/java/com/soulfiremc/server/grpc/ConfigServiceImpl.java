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

import com.soulfiremc.grpc.generated.ClientDataRequest;
import com.soulfiremc.grpc.generated.ClientPlugin;
import com.soulfiremc.grpc.generated.ConfigServiceGrpc;
import com.soulfiremc.grpc.generated.UIClientDataResponse;
import com.soulfiremc.server.SoulFireServer;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Collection;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConfigServiceImpl extends ConfigServiceGrpc.ConfigServiceImplBase {
  private final SoulFireServer soulFireServer;

  private Collection<ClientPlugin> getExtensions() {
    var plugins = new ArrayList<ClientPlugin>();
    for (var pluginWrapper : soulFireServer.pluginManager().getPlugins()) {
      var id = pluginWrapper.getPluginId();
      var description = pluginWrapper.getDescriptor().getPluginDescription();
      var version = pluginWrapper.getDescriptor().getVersion();
      var provider = pluginWrapper.getDescriptor().getProvider();

      plugins.add(
        ClientPlugin.newBuilder()
          .setId(id)
          .setDescription(description)
          .setVersion(version)
          .setProvider(provider)
          .build());
    }

    return plugins;
  }

  @Override
  public void getUIClientData(
    ClientDataRequest request, StreamObserver<UIClientDataResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().canAccessOrThrow(Resource.SERVER_CONFIG);

    try {
      var username = ServerRPCConstants.CLIENT_ID_CONTEXT_KEY.get();
      responseObserver.onNext(
        UIClientDataResponse.newBuilder()
          .setUsername(username)
          .addAllPlugins(getExtensions())
          .addAllPluginSettings(soulFireServer.settingsRegistry().exportSettingsMeta())
          .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting client data", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
