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
package net.pistonmaster.soulfire.server.plugins;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.soulfire.server.api.ExecutorHelper;
import net.pistonmaster.soulfire.server.api.PluginHelper;
import net.pistonmaster.soulfire.server.api.SoulFireAPI;
import net.pistonmaster.soulfire.server.api.event.bot.BotJoinedEvent;
import net.pistonmaster.soulfire.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.soulfire.server.data.ItemType;
import net.pistonmaster.soulfire.server.settings.lib.SettingsObject;
import net.pistonmaster.soulfire.server.settings.lib.property.BooleanProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.MinMaxPropertyLink;
import net.pistonmaster.soulfire.server.settings.lib.property.Property;
import net.pistonmaster.soulfire.server.util.TimeUtil;

import java.util.concurrent.TimeUnit;

public class AutoTotem implements InternalExtension {
    public static void onJoined(BotJoinedEvent event) {
        var connection = event.connection();
        var settingsHolder = connection.settingsHolder();
        if (!settingsHolder.get(AutoTotemSettings.ENABLED)) {
            return;
        }

        var executor = connection.executorManager().newScheduledExecutorService(connection, "AutoTotem");
        ExecutorHelper.executeRandomDelaySeconds(executor, () -> {
            var sessionDataManager = connection.sessionDataManager();
            var inventoryManager = sessionDataManager.inventoryManager();
            var playerInventory = inventoryManager.getPlayerInventory();
            var offhandSlot = playerInventory.getOffhand();

            // We only want to use totems if there are no items in the offhand
            if (offhandSlot.item() != null) {
                return;
            }

            for (var slot : playerInventory.storage()) {
                if (slot.item() == null) {
                    continue;
                }

                var item = slot.item();
                if (item.type() == ItemType.TOTEM_OF_UNDYING) {
                    if (!inventoryManager.tryInventoryControl()) {
                        return;
                    }

                    try {
                        inventoryManager.leftClickSlot(slot.slot());
                        TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
                        inventoryManager.leftClickSlot(offhandSlot.slot());
                    } finally {
                        inventoryManager.unlockInventoryControl();
                    }
                    return;
                }
            }
        }, settingsHolder.get(AutoTotemSettings.DELAY.min()), settingsHolder.get(AutoTotemSettings.DELAY.max()));
    }

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(AutoTotemSettings.class, "Auto Totem");
    }

    @Override
    public void onLoad() {
        SoulFireAPI.registerListeners(AutoTotem.class);
        PluginHelper.registerBotEventConsumer(BotJoinedEvent.class, AutoTotem::onJoined);
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class AutoTotemSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("auto-totem");
        public static final BooleanProperty ENABLED = BUILDER.ofBoolean(
                "enabled",
                "Enable Auto Totem",
                new String[]{"--auto-totem"},
                "Always put available totems in the offhand slot",
                true
        );
        public static final MinMaxPropertyLink DELAY = new MinMaxPropertyLink(
                BUILDER.ofInt(
                        "min-delay",
                        "Min delay (seconds)",
                        new String[]{"--totem-min-delay"},
                        "Minimum delay between using totems",
                        1,
                        0,
                        Integer.MAX_VALUE,
                        1
                ),
                BUILDER.ofInt(
                        "max-delay",
                        "Max delay (seconds)",
                        new String[]{"--totem-max-delay"},
                        "Maximum delay between using totems",
                        2,
                        0,
                        Integer.MAX_VALUE,
                        1
                )
        );
    }
}
