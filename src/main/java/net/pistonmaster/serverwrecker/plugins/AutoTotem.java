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
package net.pistonmaster.serverwrecker.plugins;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.serverwrecker.api.ExecutorHelper;
import net.pistonmaster.serverwrecker.api.PluginHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.bot.BotJoinedEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.serverwrecker.data.ItemType;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.settings.lib.property.MinMaxPropertyLink;
import net.pistonmaster.serverwrecker.settings.lib.property.Property;
import net.pistonmaster.serverwrecker.util.TimeUtil;

import java.util.concurrent.TimeUnit;

public class AutoTotem implements InternalExtension {
    public static void onJoined(BotJoinedEvent event) {
        var connection = event.connection();
        var settingsHolder = connection.settingsHolder();
        if (!settingsHolder.get(AutoTotemSettings.AUTO_TOTEM)) {
            return;
        }

        var executor = connection.executorManager().newScheduledExecutorService("AutoTotem");
        ExecutorHelper.executeRandomDelaySeconds(executor, () -> {
            var sessionDataManager = connection.sessionDataManager();
            var inventoryManager = sessionDataManager.getInventoryManager();
            var playerInventory = inventoryManager.getPlayerInventory();
            var offhandSlot = playerInventory.getOffhand();

            // We only want to use totems if there are no items in the offhand
            if (offhandSlot.item() != null) {
                return;
            }

            for (var slot : playerInventory.getStorage()) {
                if (slot.item() == null) {
                    continue;
                }

                var item = slot.item();
                if (item.getType() == ItemType.TOTEM_OF_UNDYING) {
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
        ServerWreckerAPI.registerListeners(AutoTotem.class);
        PluginHelper.registerBotEventConsumer(BotJoinedEvent.class, AutoTotem::onJoined);
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class AutoTotemSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("auto-totem");
        public static final BooleanProperty AUTO_TOTEM = BUILDER.ofBoolean("auto-totem",
                "Do Auto Totem?",
                "Do Auto Totem?",
                new String[]{"--auto-totem"},
                true
        );
        public static final MinMaxPropertyLink DELAY = new MinMaxPropertyLink(
                BUILDER.ofInt("totem-min-delay",
                        "Min delay (seconds)",
                        "Minimum delay between using totems",
                        new String[]{"--totem-min-delay"},
                        1,
                        0,
                        Integer.MAX_VALUE,
                        1
                ),
                BUILDER.ofInt("totem-max-delay",
                        "Max delay (seconds)",
                        "Maximum delay between using totems",
                        new String[]{"--totem-max-delay"},
                        2,
                        0,
                        Integer.MAX_VALUE,
                        1
                )
        );
    }
}
