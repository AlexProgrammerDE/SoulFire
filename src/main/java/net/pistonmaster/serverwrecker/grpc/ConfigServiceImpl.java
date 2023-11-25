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
import net.pistonmaster.serverwrecker.ServerWreckerBootstrap;
import net.pistonmaster.serverwrecker.ServerWreckerServer;
import net.pistonmaster.serverwrecker.grpc.generated.ClientDataRequest;
import net.pistonmaster.serverwrecker.grpc.generated.ClientPlugin;
import net.pistonmaster.serverwrecker.grpc.generated.ConfigServiceGrpc;
import net.pistonmaster.serverwrecker.grpc.generated.UIClientDataResponse;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConfigServiceImpl extends ConfigServiceGrpc.ConfigServiceImplBase {
    private final ServerWreckerServer serverWreckerServer;

    private static Collection<ClientPlugin> getExtensions() {
        var plugins = new ArrayList<ClientPlugin>();
        for (var pluginWrapper : ServerWreckerBootstrap.PLUGIN_MANAGER.getPlugins()) {
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
                            .build()
            );
        }

        return plugins;
    }

    @Override
    public void getUIClientData(ClientDataRequest request, StreamObserver<UIClientDataResponse> responseObserver) {
        var username = Constant.CLIENT_ID_CONTEXT_KEY.get();
        responseObserver.onNext(
                UIClientDataResponse.newBuilder()
                        .setUsername(username)
                        .addAllPlugins(getExtensions())
                        .addAllPluginSettings(serverWreckerServer.getSettingsRegistry().exportSettingsMeta())
                        .build()
        );
        responseObserver.onCompleted();
    }
}
