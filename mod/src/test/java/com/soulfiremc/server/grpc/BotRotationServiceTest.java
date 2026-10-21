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
package com.soulfiremc.server.grpc;

import com.soulfiremc.grpc.generated.BotSetRotationRequest;
import com.soulfiremc.grpc.generated.BotSetRotationResponse;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireScheduler;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.BotControlAPI;
import com.soulfiremc.server.bot.BotControlLeaseManager;
import com.soulfiremc.server.bot.BotRotationController;
import com.soulfiremc.server.bot.ControlStopReason;
import com.soulfiremc.server.bot.ControlTask;
import com.soulfiremc.server.settings.lib.InstanceSettingsDelegate;
import com.soulfiremc.server.user.SoulFireUser;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

final class BotRotationServiceTest {
  private final UUID instanceId = UUID.randomUUID();
  private final UUID botId = UUID.randomUUID();
  private final SoulFireServer server = mock();
  private final BotRotationController rotation = mock();
  private final BotControlAPI control = mock();
  private final BotServiceImpl service = new BotServiceImpl(server);
  private final Context context = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, mock(SoulFireUser.class));

  @BeforeEach
  void setUp() {
    var instance = mock(InstanceManager.class);
    var settings = mock(InstanceSettingsDelegate.class);
    var bot = mock(BotConnection.class);
    var minecraft = mock(Minecraft.class);
    minecraft.player = mock(LocalPlayer.class);
    when(server.getInstance(instanceId)).thenReturn(Optional.of(instance));
    when(instance.settingsSource()).thenReturn(settings);
    when(settings.accounts()).thenReturn(Map.of(botId, mock(MinecraftAccount.class)));
    when(instance.botControlLeaseManager()).thenReturn(new BotControlLeaseManager());
    when(instance.botConnections()).thenReturn(Map.of(botId, bot));
    when(bot.minecraft()).thenReturn(minecraft);
    when(bot.rotationControl()).thenReturn(rotation);
    when(bot.botControl()).thenReturn(control);
    when(bot.runnableWrapper()).thenReturn((SoulFireScheduler.RunnableWrapper) runnable -> runnable);
    doAnswer(invocation -> {
      ControlTask task = invocation.getArgument(0);
      task.tick();
      task.onStopped(ControlStopReason.COMPLETED, null);
      return null;
    }).when(control).replace(any());
  }

  @Test
  void largeFiniteAnglesReachTheControllerWithoutAnUnboundedLoop() {
    for (var yaw : new float[]{Float.MAX_VALUE, -Float.MAX_VALUE, 1.0E20F}) {
      var request = request(yaw, Float.MAX_VALUE);
      StreamObserver<BotSetRotationResponse> observer = mock();

      assertTimeoutPreemptively(Duration.ofSeconds(2), () -> context.run(() -> service.setRotation(request, observer)));

      verify(rotation).lookTo(yaw, Float.MAX_VALUE);
      verify(observer).onNext(BotSetRotationResponse.newBuilder().setSuccess(true).build());
      verify(observer).onCompleted();
    }
  }

  @Test
  void nonFiniteAnglesAreRejectedBeforeSchedulingAnAction() {
    for (var value : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
      for (var request : new BotSetRotationRequest[]{request(value, 0), request(0, value)}) {
        var error = assertThrows(StatusRuntimeException.class,
          () -> context.run(() -> service.setRotation(request, mock())));
        assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
      }
    }
    verifyNoInteractions(rotation, control);
  }

  private BotSetRotationRequest request(float yaw, float pitch) {
    return BotSetRotationRequest.newBuilder()
      .setInstanceId(instanceId.toString())
      .setBotId(botId.toString())
      .setYaw(yaw)
      .setPitch(pitch)
      .build();
  }
}
