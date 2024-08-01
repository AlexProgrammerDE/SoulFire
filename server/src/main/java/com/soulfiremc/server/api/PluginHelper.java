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
package com.soulfiremc.server.api;

import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.event.SoulFireAttackEvent;
import com.soulfiremc.server.api.event.SoulFireBotEvent;
import com.soulfiremc.server.api.event.attack.BotConnectionInitEvent;
import com.soulfiremc.server.api.event.attack.InstanceInitEvent;
import java.util.function.Consumer;
import net.lenni0451.lambdaevents.LambdaManager;

/**
 * This class contains helper methods for plugins to use to make their life easier.
 */
public class PluginHelper {
  private PluginHelper() {}

  /**
   * Registers a consumer that is called on its event on every bot of every attack. This skips the
   * boilerplate of creating a listener and subscribing to the events of both the attack manager
   * init and pre bot connect. The only reason for this to exist is to streamline the process of
   * creating a bot listener. Since most plugins only hook into the bot connection and not any
   * global or attack events, this is the easiest way to do it.
   *
   * @param soulFireServer The instance of the SoulFire server.
   * @param clazz    The class of the bot event.
   * @param consumer The consumer that is called when the event is posted.
   * @param <T>      The type of the bot event.
   * @see #registerAttackEventConsumer(SoulFireServer, Class, Consumer)
   */
  public static <T extends SoulFireBotEvent> void registerBotEventConsumer(
    SoulFireServer soulFireServer,
    Class<T> clazz, Consumer<T> consumer) {
    registerAttackEventConsumer(
      soulFireServer,
      BotConnectionInitEvent.class,
      event -> event.connection().registerListener(clazz, consumer));
  }

  /**
   * Registers a consumer that is called when a specific attack event is posted.
   *
   * @param soulFireServer The instance of the SoulFire server.
   * @param clazz    The class of the attack event.
   * @param consumer The consumer that is called when the event is posted.
   * @param <T>      The type of the attack event.
   */
  public static <T extends SoulFireAttackEvent> void registerAttackEventConsumer(
    SoulFireServer soulFireServer,
    Class<T> clazz, Consumer<T> consumer) {
    soulFireServer.registerListener(
      InstanceInitEvent.class,
      event -> event.instanceManager().registerListener(clazz, consumer));
  }

  public static <T extends SoulFireAttackEvent> void registerSafeEventConsumer(
    LambdaManager eventBus, Class<T> clazz, Consumer<T> consumer) {
    eventBus.registerConsumer(consumer, clazz);
  }
}
