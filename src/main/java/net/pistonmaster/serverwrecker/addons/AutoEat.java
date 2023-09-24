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
package net.pistonmaster.serverwrecker.addons;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.AddonCLIHelper;
import net.pistonmaster.serverwrecker.api.AddonHelper;
import net.pistonmaster.serverwrecker.api.ExecutorHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.GlobalEventHandler;
import net.pistonmaster.serverwrecker.api.event.bot.BotJoinedEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.data.DangerFood;
import net.pistonmaster.serverwrecker.data.FoodType;
import net.pistonmaster.serverwrecker.data.ItemType;
import net.pistonmaster.serverwrecker.gui.libs.JMinMaxHelper;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.bot.container.ContainerSlot;
import net.pistonmaster.serverwrecker.protocol.bot.container.InventoryManager;
import net.pistonmaster.serverwrecker.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.serverwrecker.protocol.bot.model.HealthData;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import net.pistonmaster.serverwrecker.util.TimeUtil;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoEat implements InternalAddon {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
        AddonHelper.registerBotEventConsumer(BotJoinedEvent.class, this::onJoined);
    }

    public void onJoined(BotJoinedEvent event) {
        BotConnection connection = event.connection();
        if (!connection.settingsHolder().has(AutoEatSettings.class)) {
            return;
        }

        AutoEatSettings settings = connection.settingsHolder().get(AutoEatSettings.class);
        if (!settings.autoEat()) {
            return;
        }

        ScheduledExecutorService executor = connection.executorManager().newScheduledExecutorService("AutoEat");
        ExecutorHelper.executeRandomDelaySeconds(executor, () -> {
            SessionDataManager sessionDataManager = connection.sessionDataManager();

            HealthData healthData = sessionDataManager.getHealthData();
            if (healthData == null || healthData.food() >= 20) {
                return;
            }

            InventoryManager inventoryManager = sessionDataManager.getInventoryManager();
            PlayerInventoryContainer playerInventory = inventoryManager.getPlayerInventory();

            int i = 0;
            for (ContainerSlot slot : playerInventory.getHotbar()) {
                int hotbarSlot = i++;

                if (slot.item() == null) {
                    continue;
                }

                ItemType itemType = slot.item().getType();
                FoodType foodType = FoodType.VALUES.stream()
                        .filter(type -> type.itemType() == itemType)
                        .max((o1, o2) -> Double.compare(o2.effectiveQuality(), o1.effectiveQuality()))
                        .orElse(null);

                if (foodType == null || DangerFood.isDangerFood(foodType)) {
                    continue;
                }

                if (!inventoryManager.tryInventoryControl()) {
                    return;
                }

                try {
                    inventoryManager.setHeldItemSlot(hotbarSlot);
                    inventoryManager.sendHeldItemChange();
                    sessionDataManager.getBotActionManager().useItemInHand(Hand.MAIN_HAND);

                    // Wait before eating again
                    TimeUtil.waitTime(2, TimeUnit.SECONDS);
                    return;
                } finally {
                    inventoryManager.unlockInventoryControl();
                }
            }

            for (ContainerSlot slot : playerInventory.getMainInventory()) {
                if (slot.item() == null) {
                    continue;
                }

                ItemType itemType = slot.item().getType();
                FoodType foodType = FoodType.VALUES.stream()
                        .filter(type -> type.itemType() == itemType)
                        .max((o1, o2) -> Double.compare(o2.effectiveQuality(), o1.effectiveQuality()))
                        .orElse(null);

                if (foodType == null || DangerFood.isDangerFood(foodType)) {
                    continue;
                }

                if (!inventoryManager.tryInventoryControl()) {
                    return;
                }

                try {
                    inventoryManager.leftClickSlot(slot.slot());
                    inventoryManager.leftClickSlot(playerInventory.getHotbarSlot(inventoryManager.getHeldItemSlot()).slot());
                    if (inventoryManager.getCursorItem() != null) {
                        inventoryManager.leftClickSlot(slot.slot());
                    }

                    // Wait before eating again
                    TimeUtil.waitTime(2, TimeUnit.SECONDS);
                    sessionDataManager.getBotActionManager().useItemInHand(Hand.MAIN_HAND);
                    return;
                } finally {
                    inventoryManager.unlockInventoryControl();
                }
            }
        }, settings.minDelay(), settings.maxDelay());
    }

    @GlobalEventHandler
    public void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new AutoEatPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @GlobalEventHandler
    public void onCommandLine(CommandManagerInitEvent event) {
        AddonCLIHelper.registerCommands(event.commandLine(), AutoEatSettings.class, new AutoEatCommand());
    }

    private static class AutoEatPanel extends NavigationItem implements SettingsDuplex<AutoEatSettings> {
        private final JCheckBox autoEat;
        private final JSpinner minDelay;
        private final JSpinner maxDelay;

        AutoEatPanel(ServerWrecker serverWrecker) {
            super();
            serverWrecker.getSettingsManager().registerDuplex(AutoEatSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Do Auto Eat?"));
            autoEat = new PresetJCheckBox(AutoEatSettings.DEFAULT_AUTO_EAT);
            add(autoEat);

            add(new JLabel("Min Delay (Seconds)"));
            minDelay = new JSpinner(new SpinnerNumberModel(AutoEatSettings.DEFAULT_MIN_DELAY, 1, 1000, 1));
            add(minDelay);

            add(new JLabel("Max Delay (Seconds)"));
            maxDelay = new JSpinner(new SpinnerNumberModel(AutoEatSettings.DEFAULT_MAX_DELAY, 1, 1000, 1));
            add(maxDelay);

            JMinMaxHelper.applyLink(minDelay, maxDelay);
        }

        @Override
        public String getNavigationName() {
            return "Auto Eat";
        }

        @Override
        public String getNavigationId() {
            return "auto-eat";
        }

        @Override
        public void onSettingsChange(AutoEatSettings settings) {
            autoEat.setSelected(settings.autoEat());
            minDelay.setValue(settings.minDelay());
            maxDelay.setValue(settings.maxDelay());
        }

        @Override
        public AutoEatSettings collectSettings() {
            return new AutoEatSettings(
                    autoEat.isSelected(),
                    (int) minDelay.getValue(),
                    (int) maxDelay.getValue()
            );
        }
    }

    private static class AutoEatCommand implements SettingsProvider<AutoEatSettings> {
        @CommandLine.Option(names = {"--auto-eat"}, description = "Do auto eat?")
        private boolean autoEat = AutoEatSettings.DEFAULT_AUTO_EAT;
        @CommandLine.Option(names = {"--eat-min-delay"}, description = "Minimum delay between eating")
        private int minDelay = AutoEatSettings.DEFAULT_MIN_DELAY;
        @CommandLine.Option(names = {"--eat-max-delay"}, description = "Maximum delay between eating")
        private int maxDelay = AutoEatSettings.DEFAULT_MAX_DELAY;

        @Override
        public AutoEatSettings collectSettings() {
            return new AutoEatSettings(
                    autoEat,
                    minDelay,
                    maxDelay
            );
        }
    }

    private record AutoEatSettings(
            boolean autoEat,
            int minDelay,
            int maxDelay
    ) implements SettingsObject {
        public static final boolean DEFAULT_AUTO_EAT = true;
        public static final int DEFAULT_MIN_DELAY = 1;
        public static final int DEFAULT_MAX_DELAY = 2;
    }
}
