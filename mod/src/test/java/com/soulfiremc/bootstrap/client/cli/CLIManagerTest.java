/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.bootstrap.client.cli;

import com.soulfiremc.bootstrap.client.grpc.RPCClient;
import com.soulfiremc.grpc.generated.InstanceInfo;
import com.soulfiremc.grpc.generated.InstanceInfoResponse;
import com.soulfiremc.grpc.generated.InstanceListResponse;
import com.soulfiremc.grpc.generated.InstanceServiceGrpc;
import com.soulfiremc.grpc.generated.IntSetting;
import com.soulfiremc.grpc.generated.MCAuthServiceGrpc;
import com.soulfiremc.grpc.generated.SettingsDefinition;
import com.soulfiremc.grpc.generated.SettingsEntryIdentifier;
import com.soulfiremc.grpc.generated.SettingsPage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class CLIManagerTest {
  @ParameterizedTest
  @MethodSource("commands")
  void returnsParserExitCodesWithoutChangingTheSession(String[] arguments, int expectedExitCode) {
    var rpc = mock(RPCClient.class);
    var stub = mock(InstanceServiceGrpc.InstanceServiceBlockingStub.class);
    when(rpc.instanceStubBlocking()).thenReturn(stub);
    when(rpc.mcAuthServiceBlocking()).thenReturn(mock(MCAuthServiceGrpc.MCAuthServiceBlockingStub.class));
    when(stub.listInstances(any())).thenReturn(InstanceListResponse.newBuilder()
      .addInstances(InstanceListResponse.Instance.newBuilder().setId(UUID.randomUUID().toString()).setFriendlyName("CLI Session"))
      .build());
    var settingId = SettingsEntryIdentifier.newBuilder().setNamespace("qa").setKey("count").build();
    when(stub.getInstanceInfo(any())).thenReturn(InstanceInfoResponse.newBuilder().setInfo(InstanceInfo.newBuilder()
      .addSettingsDefinitions(SettingsDefinition.newBuilder().setId(settingId)
        .setInt(IntSetting.newBuilder().setDef(1).setMin(0).setMax(10)))
      .addInstanceSettings(SettingsPage.newBuilder().addEntries(settingId)))
      .build());

    var cli = new CLIManager(rpc);
    try {
      assertEquals(expectedExitCode, cli.initCLI(arguments));
      verify(stub, never()).updateInstanceConfig(any());
      verify(stub, never()).createInstance(any());
    } finally {
      cli.shutdownManager().shutdownSoftware(false);
    }
  }

  private static Stream<Arguments> commands() {
    return Stream.of(
      Arguments.of(new String[]{"--help"}, 0),
      Arguments.of(new String[]{"--unknown-option"}, 2),
      Arguments.of(new String[]{"--qa-count", "invalid"}, 2),
      Arguments.of(new String[]{"--version", "--qa-count", "3"}, 0));
  }
}
