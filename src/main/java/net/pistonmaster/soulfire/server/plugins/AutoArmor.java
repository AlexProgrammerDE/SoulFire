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
import net.pistonmaster.soulfire.server.data.ArmorType;
import net.pistonmaster.soulfire.server.protocol.bot.container.ContainerSlot;
import net.pistonmaster.soulfire.server.protocol.bot.container.InventoryManager;
import net.pistonmaster.soulfire.server.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.soulfire.server.settings.lib.SettingsObject;
import net.pistonmaster.soulfire.server.settings.lib.property.BooleanProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.MinMaxPropertyLink;
import net.pistonmaster.soulfire.server.settings.lib.property.Property;
import net.pistonmaster.soulfire.server.util.TimeUtil;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AutoArmor implements InternalExtension {
    private static void putOn(InventoryManager inventoryManager, PlayerInventoryContainer inventory, ContainerSlot targetSlot, ArmorType armorType) {
        var bestItem = Arrays.stream(inventory.storage()).filter(s -> {
            if (s.item() == null) {
                return false;
            }

            return armorType.itemTypes().contains(s.item().type());
        }).reduce((first, second) -> {
            assert first.item() != null;

            var firstIndex = armorType.itemTypes().indexOf(first.item().type());
            var secondIndex = armorType.itemTypes().indexOf(second.item().type());

            return firstIndex > secondIndex ? first : second;
        });

        if (bestItem.isEmpty() || bestItem.get().item() == null) {
            return;
        }

        if (targetSlot.item() != null) {
            var targetIndex = armorType.itemTypes().indexOf(targetSlot.item().type());
            var bestIndex = armorType.itemTypes().indexOf(bestItem.get().item().type());

            if (targetIndex >= bestIndex) {
                return;
            }
        }

        bestItem.ifPresent(bestItemSlot -> {
            if (!inventoryManager.tryInventoryControl()) {
                return;
            }

            try {
                inventoryManager.leftClickSlot(bestItemSlot.slot());
                TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
                inventoryManager.leftClickSlot(targetSlot.slot());
                TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);

                if (inventoryManager.cursorItem() != null) {
                    inventoryManager.leftClickSlot(bestItemSlot.slot());
                    TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
                }
            } finally {
                inventoryManager.unlockInventoryControl();
            }
        });
    }

    public static void onJoined(BotJoinedEvent event) {
        var connection = event.connection();
        var settingsHolder = connection.settingsHolder();
        if (!settingsHolder.get(AutoArmorSettings.ENABLED)) {
            return;
        }

        var executor = connection.executorManager().newScheduledExecutorService(connection, "AutoJump");
        ExecutorHelper.executeRandomDelaySeconds(executor, () -> {
            var sessionDataManager = connection.sessionDataManager();
            var inventoryManager = sessionDataManager.inventoryManager();
            var playerInventory = inventoryManager.getPlayerInventory();

            var armorTypes = Map.of(
                    ArmorType.HELMET, playerInventory.getHelmet(),
                    ArmorType.CHESTPLATE, playerInventory.getChestplate(),
                    ArmorType.LEGGINGS, playerInventory.getLeggings(),
                    ArmorType.BOOTS, playerInventory.getBoots()
            );

            for (var entry : armorTypes.entrySet()) {
                putOn(inventoryManager, playerInventory, entry.getValue(), entry.getKey());
            }
        }, settingsHolder.get(AutoArmorSettings.DELAY.min()), settingsHolder.get(AutoArmorSettings.DELAY.max()));
    }

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(AutoArmorSettings.class, "Auto Armor");
    }

    @Override
    public void onLoad() {
        SoulFireAPI.registerListeners(AutoArmor.class);
        PluginHelper.registerBotEventConsumer(BotJoinedEvent.class, AutoArmor::onJoined);
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private static class AutoArmorSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("auto-armor");
        public static final BooleanProperty ENABLED = BUILDER.ofBoolean(
                "enabled",
                "Enable Auto Armor",
                new String[]{"--auto-armor"},
                "Put on best armor automatically",
                true
        );
        public static final MinMaxPropertyLink DELAY = new MinMaxPropertyLink(
                BUILDER.ofInt(
                        "min-delay",
                        "Min delay (seconds)",
                        new String[]{"--armor-min-delay"},
                        "Minimum delay between putting on armor",
                        1,
                        0,
                        Integer.MAX_VALUE,
                        1
                ),
                BUILDER.ofInt(
                        "max-delay",
                        "Max delay (seconds)",
                        new String[]{"--armor-max-delay"},
                        "Maximum delay between putting on armor",
                        2,
                        0,
                        Integer.MAX_VALUE,
                        1
                )
        );
    }
}
