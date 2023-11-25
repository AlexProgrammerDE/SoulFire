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
package net.pistonmaster.serverwrecker.api;

import net.pistonmaster.serverwrecker.api.event.ServerWreckerAttackEvent;
import net.pistonmaster.serverwrecker.api.event.ServerWreckerBotEvent;
import net.pistonmaster.serverwrecker.api.event.attack.AttackInitEvent;
import net.pistonmaster.serverwrecker.api.event.attack.BotConnectionInitEvent;

import java.util.function.Consumer;

/**
 * This class contains helper methods for plugins to use to make their life easier.
 */
public class PluginHelper {
    private PluginHelper() {
    }

    /**
     * Registers a consumer that is called on its event on every bot of every attack.
     * This skips the boilerplate of creating a listener and subscribing to the events of both the attack manager init and pre bot connect.
     * The only reason for this to exist is to streamline the process of creating a bot listener.
     * Since most plugins only hook into the bot connection and not any global or attack events, this is the easiest way to do it.
     *
     * @param clazz    The class of the bot event.
     * @param consumer The consumer that is called when the event is posted.
     * @param <T>      The type of the bot event.
     * @see #registerAttackEventConsumer(Class, Consumer)
     */
    public static <T extends ServerWreckerBotEvent> void registerBotEventConsumer(Class<T> clazz, Consumer<T> consumer) {
        registerAttackEventConsumer(BotConnectionInitEvent.class, event ->
                event.connection().eventBus().register(clazz, consumer));
    }

    /**
     * Registers a consumer that is called when a specific attack event is posted.
     *
     * @param clazz    The class of the attack event.
     * @param consumer The consumer that is called when the event is posted.
     * @param <T>      The type of the attack event.
     */
    public static <T extends ServerWreckerAttackEvent> void registerAttackEventConsumer(Class<T> clazz, Consumer<T> consumer) {
        ServerWreckerAPI.registerListener(AttackInitEvent.class, event ->
                event.attackManager().getEventBus().register(clazz, consumer));
    }
}
