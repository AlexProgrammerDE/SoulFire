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
import net.pistonmaster.serverwrecker.api.event.lifecycle.SettingsManagerInitEvent;
import net.pistonmaster.serverwrecker.data.ArmorType;
import net.pistonmaster.serverwrecker.protocol.bot.container.ContainerSlot;
import net.pistonmaster.serverwrecker.protocol.bot.container.InventoryManager;
import net.pistonmaster.serverwrecker.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.settings.lib.property.IntProperty;
import net.pistonmaster.serverwrecker.settings.lib.property.MinMaxPropertyLink;
import net.pistonmaster.serverwrecker.settings.lib.property.Property;
import net.pistonmaster.serverwrecker.util.TimeUtil;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AutoArmor implements InternalExtension {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(AutoArmor.class);
        PluginHelper.registerBotEventConsumer(BotJoinedEvent.class, AutoArmor::onJoined);
    }

    private static void putOn(InventoryManager inventoryManager, PlayerInventoryContainer inventory, ContainerSlot targetSlot, ArmorType armorType) {
        var bestItem = Arrays.stream(inventory.getStorage()).filter(s -> {
            if (s.item() == null) {
                return false;
            }

            return armorType.getItemTypes().contains(s.item().getType());
        }).reduce((first, second) -> {
            assert first.item() != null;

            var firstIndex = armorType.getItemTypes().indexOf(first.item().getType());
            var secondIndex = armorType.getItemTypes().indexOf(second.item().getType());

            return firstIndex > secondIndex ? first : second;
        });

        if (bestItem.isEmpty() || bestItem.get().item() == null) {
            return;
        }

        if (targetSlot.item() != null) {
            var targetIndex = armorType.getItemTypes().indexOf(targetSlot.item().getType());
            var bestIndex = armorType.getItemTypes().indexOf(bestItem.get().item().getType());

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

                if (inventoryManager.getCursorItem() != null) {
                    inventoryManager.leftClickSlot(bestItemSlot.slot());
                    TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
                }
            } finally {
                inventoryManager.unlockInventoryControl();
            }
        });
    }

    public static void onJoined(BotJoinedEvent event) {
        var settingsHolder = event.connection().settingsHolder();
        if (!settingsHolder.get(AutoArmorSettings.AUTO_ARMOR)) {
            return;
        }

        var executor = event.connection().executorManager().newScheduledExecutorService("AutoJump");
        var connection = event.connection();
        ExecutorHelper.executeRandomDelaySeconds(executor, () -> {
            var sessionDataManager = connection.sessionDataManager();
            var inventoryManager = sessionDataManager.getInventoryManager();
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
        }, settingsHolder.get(AutoArmorSettings.MIN_DELAY), settingsHolder.get(AutoArmorSettings.MAX_DELAY));
    }

    @EventHandler
    public static void onPluginPanel(SettingsManagerInitEvent event) {
        event.settingsManager().addClass(AutoArmorSettings.class);
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private static class AutoArmorSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("auto-armor");
        public static final BooleanProperty AUTO_ARMOR = BUILDER.ofBoolean("auto-armor",
                "Do Auto Armor?",
                "Do Auto Armor?",
                new String[]{"--auto-armor"},
                true
        );
        public static final IntProperty MIN_DELAY = BUILDER.ofInt("armor-min-delay",
                "Min delay (seconds)",
                "Minimum delay between putting on armor",
                new String[]{"--armor-min-delay"},
                1
        );
        public static final IntProperty MAX_DELAY = BUILDER.ofInt("armor-max-delay",
                "Max delay (seconds)",
                "Maximum delay between putting on armor",
                new String[]{"--armor-max-delay"},
                2
        );
        public static final MinMaxPropertyLink DELAY = new MinMaxPropertyLink(MIN_DELAY, MAX_DELAY);
    }
}
