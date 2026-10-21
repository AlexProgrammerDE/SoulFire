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

import com.soulfiremc.grpc.generated.BotChatEvent;
import com.soulfiremc.grpc.generated.BotEvent;
import com.soulfiremc.grpc.generated.BotStatus;
import com.soulfiremc.grpc.generated.InstanceEvent;
import com.soulfiremc.grpc.generated.InstanceEventFilter;
import com.soulfiremc.grpc.generated.WatchBotEventsRequest;
import com.soulfiremc.grpc.generated.WatchInstanceEventsRequest;
import com.soulfiremc.server.BotStateManager;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.account.AuthType;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.account.service.OfflineJavaData;
import com.soulfiremc.server.settings.lib.InstanceSettingsDelegate;
import com.soulfiremc.server.user.SoulFireUser;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class InstanceLiveServiceTest {
  private final UUID instanceId = UUID.randomUUID();
  private final SoulFireServer server = mock();
  private final InstanceManager instance = mock();
  private final BotStateManager states = mock();
  private final BotLiveServiceImpl botLiveService = mock();
  private final ServerCallStreamObserver<InstanceEvent> parent = mock();
  private final Map<UUID, MinecraftAccount> accounts = new HashMap<>();
  private final Map<UUID, ServerCallStreamObserver<BotEvent>> children = new HashMap<>();
  private final List<Consumer<BotStateManager.StatusEvent>> listeners = new ArrayList<>();
  private final List<InstanceEvent> received = new ArrayList<>();
  private final AtomicInteger subscriptions = new AtomicInteger();
  private final AtomicInteger cancellations = new AtomicInteger();
  private Runnable cancelParent;

  @BeforeEach
  void setUp() {
    when(server.getInstance(instanceId)).thenReturn(Optional.of(instance));
    when(instance.botStateManager()).thenReturn(states);
    var settings = mock(InstanceSettingsDelegate.class);
    when(settings.accounts()).thenReturn(accounts);
    when(instance.settingsSource()).thenReturn(settings);
    when(states.addStatusListener(any())).thenAnswer(invocation -> {
      Consumer<BotStateManager.StatusEvent> listener = invocation.getArgument(0);
      listeners.add(listener);
      return (Runnable) () -> listeners.remove(listener);
    });
    when(parent.isReady()).thenReturn(true);
    doAnswer(invocation -> {
      cancelParent = invocation.getArgument(0);
      return null;
    }).when(parent).setOnCancelHandler(any());
    doAnswer(invocation -> {
      received.add(invocation.getArgument(0));
      return null;
    }).when(parent).onNext(any());
    doAnswer(invocation -> {
      WatchBotEventsRequest request = invocation.getArgument(0);
      ServerCallStreamObserver<BotEvent> child = invocation.getArgument(1);
      children.put(UUID.fromString(request.getBotId()), child);
      subscriptions.incrementAndGet();
      child.setOnCancelHandler(cancellations::incrementAndGet);
      return null;
    }).when(botLiveService).watchBotEvents(any(), any());
  }

  @Test
  void openStreamFollowsAddedRemovedAndReaddedAccounts() {
    watch(InstanceEventFilter.newBuilder().setIncludeChat(true).build());
    assertEquals(1, listeners.size());
    var account = account();
    accounts.put(account.profileId(), account);
    update(account.profileId());
    update(account.profileId());
    assertEquals(1, subscriptions.get());
    var first = children.get(account.profileId());
    first.onNext(BotEvent.newBuilder().setChat(BotChatEvent.getDefaultInstance()).build());
    assertEquals(1, received.size());
    assertEquals(account.profileId().toString(), received.getFirst().getBotProfileId());
    assertEquals(InstanceEvent.EventCase.CHAT, received.getFirst().getEventCase());

    accounts.remove(account.profileId());
    List.copyOf(listeners).forEach(listener -> listener.accept(BotStateManager.StatusEvent.removed(account.profileId())));
    assertTrue(first.isCancelled());
    first.onNext(BotEvent.newBuilder().setChat(BotChatEvent.getDefaultInstance()).build());
    assertEquals(1, received.size());

    accounts.put(account.profileId(), account);
    update(account.profileId());
    assertEquals(2, subscriptions.get());
    assertNotSame(first, children.get(account.profileId()));
    cancelParent.run();
    cancelParent.run();
    assertTrue(listeners.isEmpty());
    assertEquals(2, cancellations.get());
  }

  @Test
  void allowlistAppliesToAccountsAddedAfterSubscription() {
    var selected = account();
    var ignored = account();
    accounts.put(ignored.profileId(), ignored);
    watch(InstanceEventFilter.newBuilder().addBotIds(selected.profileId().toString()).build());
    accounts.put(selected.profileId(), selected);
    update(ignored.profileId());
    update(selected.profileId());
    assertEquals(1, children.size());
    assertTrue(children.containsKey(selected.profileId()));
    assertFalse(children.containsKey(ignored.profileId()));
    cancelParent.run();
    assertEquals(1, cancellations.get());
  }

  @Test
  void failedInitialChildSetupReleasesTheFleetListener() {
    for (var account : List.of(account(), account())) {
      accounts.put(account.profileId(), account);
    }
    doAnswer(invocation -> {
      if (subscriptions.getAndIncrement() > 0) {
        throw Status.NOT_FOUND.asRuntimeException();
      }
      ServerCallStreamObserver<BotEvent> child = invocation.getArgument(1);
      child.setOnCancelHandler(cancellations::incrementAndGet);
      return null;
    }).when(botLiveService).watchBotEvents(any(), any());
    assertThrows(StatusRuntimeException.class, () -> watch(InstanceEventFilter.getDefaultInstance()));
    assertTrue(listeners.isEmpty());
    assertEquals(1, cancellations.get());
  }

  @Test
  void cancellationDuringChildSetupReleasesLateRegisteredCleanup() {
    var account = account();
    accounts.put(account.profileId(), account);
    doAnswer(invocation -> {
      ServerCallStreamObserver<BotEvent> child = invocation.getArgument(1);
      cancelParent.run();
      child.setOnCancelHandler(cancellations::incrementAndGet);
      assertTrue(child.isCancelled());
      return null;
    }).when(botLiveService).watchBotEvents(any(), any());

    watch(InstanceEventFilter.getDefaultInstance());

    assertTrue(listeners.isEmpty());
    assertEquals(1, cancellations.get());
  }

  private void watch(InstanceEventFilter filter) {
    var context = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, mock(SoulFireUser.class));
    context.run(() -> new InstanceLiveServiceImpl(server, botLiveService).watchInstanceEvents(
      WatchInstanceEventsRequest.newBuilder().setInstanceId(instanceId.toString()).setFilter(filter).build(), parent));
  }

  private void update(UUID botId) {
    var event = BotStateManager.StatusEvent.updated(BotStatus.newBuilder().setProfileId(botId.toString()).build());
    List.copyOf(listeners).forEach(listener -> listener.accept(event));
  }

  private static MinecraftAccount account() {
    return new MinecraftAccount(AuthType.OFFLINE, UUID.randomUUID(), UUID.randomUUID().toString(), new OfflineJavaData(), Map.of(), Map.of());
  }
}
