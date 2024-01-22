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
import net.pistonmaster.soulfire.SoulFireBootstrap;
import net.pistonmaster.soulfire.grpc.generated.ClientDataRequest;
import net.pistonmaster.soulfire.grpc.generated.ClientPlugin;
import net.pistonmaster.soulfire.grpc.generated.ConfigServiceGrpc;
import net.pistonmaster.soulfire.grpc.generated.UIClientDataResponse;
import net.pistonmaster.soulfire.server.SoulFireServer;
import net.pistonmaster.soulfire.util.RPCConstants;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConfigServiceImpl extends ConfigServiceGrpc.ConfigServiceImplBase {
    private final SoulFireServer soulFireServer;

    private static Collection<ClientPlugin> getExtensions() {
        var plugins = new ArrayList<ClientPlugin>();
        for (var pluginWrapper : SoulFireBootstrap.PLUGIN_MANAGER.getPlugins()) {
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
        var username = RPCConstants.CLIENT_ID_CONTEXT_KEY.get();
        responseObserver.onNext(
                UIClientDataResponse.newBuilder()
                        .setUsername(username)
                        .addAllPlugins(getExtensions())
                        .addAllPluginSettings(soulFireServer.settingsRegistry().exportSettingsMeta())
                        .build()
        );
        responseObserver.onCompleted();
    }
}
