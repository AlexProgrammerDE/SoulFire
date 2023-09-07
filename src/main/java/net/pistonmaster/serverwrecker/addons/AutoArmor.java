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

import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.AddonCLIHelper;
import net.pistonmaster.serverwrecker.api.ExecutorHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.EventHandler;
import net.pistonmaster.serverwrecker.api.event.bot.BotJoinedEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.data.ArmorType;
import net.pistonmaster.serverwrecker.gui.libs.JMinMaxHelper;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.bot.container.ContainerSlot;
import net.pistonmaster.serverwrecker.protocol.bot.container.InventoryManager;
import net.pistonmaster.serverwrecker.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import net.pistonmaster.serverwrecker.util.TimeUtil;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoArmor implements InternalAddon {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
    }

    @EventHandler
    public void onJoined(BotJoinedEvent event) {
        if (!event.connection().settingsHolder().has(AutoArmorSettings.class)) {
            return;
        }

        AutoArmorSettings settings = event.connection().settingsHolder().get(AutoArmorSettings.class);
        if (!settings.autoArmor()) {
            return;
        }

        new BotArmorThread(event.connection(),
                event.connection().executorManager().newScheduledExecutorService());
    }

    @EventHandler
    public void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new AutoArmorPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @EventHandler
    public void onCommandLine(CommandManagerInitEvent event) {
        AddonCLIHelper.registerCommands(event.commandLine(), AutoArmorSettings.class, new AutoArmorCommand());
    }

    private record BotArmorThread(BotConnection connection, ScheduledExecutorService executor) {
        public BotArmorThread {
            AutoArmorSettings settings = connection.settingsHolder().get(AutoArmorSettings.class);

            ExecutorHelper.executeRandomDelaySeconds(executor, () -> {
                SessionDataManager sessionDataManager = connection.sessionDataManager();
                InventoryManager inventoryManager = sessionDataManager.getInventoryManager();
                PlayerInventoryContainer playerInventory = inventoryManager.getPlayerInventory();

                Map<ArmorType, ContainerSlot> armorTypes = Map.of(
                        ArmorType.HELMET, playerInventory.getHelmet(),
                        ArmorType.CHESTPLATE, playerInventory.getChestplate(),
                        ArmorType.LEGGINGS, playerInventory.getLeggings(),
                        ArmorType.BOOTS, playerInventory.getBoots()
                );

                for (Map.Entry<ArmorType, ContainerSlot> entry : armorTypes.entrySet()) {
                    putOn(inventoryManager, playerInventory, entry.getValue(), entry.getKey());
                }
            }, settings.minDelay(), settings.maxDelay());
        }

        private void putOn(InventoryManager inventoryManager, PlayerInventoryContainer inventory, ContainerSlot targetSlot, ArmorType armorType) {
            Optional<ContainerSlot> bestItem = Arrays.stream(inventory.getStorage()).filter(s -> {
                if (s.item() == null) {
                    return false;
                }

                return armorType.getItemTypes().contains(s.item().getType());
            }).reduce((first, second) -> {
                assert first.item() != null;

                int firstIndex = armorType.getItemTypes().indexOf(first.item().getType());
                int secondIndex = armorType.getItemTypes().indexOf(second.item().getType());

                return firstIndex > secondIndex ? first : second;
            });

            if (bestItem.isEmpty() || bestItem.get().item() == null) {
                return;
            }

            if (targetSlot.item() != null) {
                int targetIndex = armorType.getItemTypes().indexOf(targetSlot.item().getType());
                int bestIndex = armorType.getItemTypes().indexOf(bestItem.get().item().getType());

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
    }

    private static class AutoArmorPanel extends NavigationItem implements SettingsDuplex<AutoArmorSettings> {
        private final JCheckBox autoArmor;
        private final JSpinner minDelay;
        private final JSpinner maxDelay;

        public AutoArmorPanel(ServerWrecker serverWrecker) {
            super();
            serverWrecker.getSettingsManager().registerDuplex(AutoArmorSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Do Auto Armor?"));
            autoArmor = new PresetJCheckBox(AutoArmorSettings.DEFAULT_AUTO_ARMOR);
            add(autoArmor);

            add(new JLabel("Min Delay (Seconds)"));
            minDelay = new JSpinner(new SpinnerNumberModel(AutoArmorSettings.DEFAULT_MIN_DELAY, 1, 1000, 1));
            add(minDelay);

            add(new JLabel("Max Delay (Seconds)"));
            maxDelay = new JSpinner(new SpinnerNumberModel(AutoArmorSettings.DEFAULT_MAX_DELAY, 1, 1000, 1));
            add(maxDelay);

            JMinMaxHelper.applyLink(minDelay, maxDelay);
        }

        @Override
        public String getNavigationName() {
            return "Auto Armor";
        }

        @Override
        public String getNavigationId() {
            return "auto-armor";
        }

        @Override
        public void onSettingsChange(AutoArmorSettings settings) {
            autoArmor.setSelected(settings.autoArmor());
            minDelay.setValue(settings.minDelay());
            maxDelay.setValue(settings.maxDelay());
        }

        @Override
        public AutoArmorSettings collectSettings() {
            return new AutoArmorSettings(
                    autoArmor.isSelected(),
                    (int) minDelay.getValue(),
                    (int) maxDelay.getValue()
            );
        }
    }

    private static class AutoArmorCommand implements SettingsProvider<AutoArmorSettings> {
        @CommandLine.Option(names = {"--auto-armor"}, description = "Do auto armor?")
        private boolean autoArmor = AutoArmorSettings.DEFAULT_AUTO_ARMOR;
        @CommandLine.Option(names = {"--armor-min-delay"}, description = "Minimum delay between putting on armor")
        private int minDelay = AutoArmorSettings.DEFAULT_MIN_DELAY;
        @CommandLine.Option(names = {"--armor-max-delay"}, description = "Maximum delay between putting on armor")
        private int maxDelay = AutoArmorSettings.DEFAULT_MAX_DELAY;

        @Override
        public AutoArmorSettings collectSettings() {
            return new AutoArmorSettings(
                    autoArmor,
                    minDelay,
                    maxDelay
            );
        }
    }

    private record AutoArmorSettings(
            boolean autoArmor,
            int minDelay,
            int maxDelay
    ) implements SettingsObject {
        public static final boolean DEFAULT_AUTO_ARMOR = true;
        public static final int DEFAULT_MIN_DELAY = 1;
        public static final int DEFAULT_MAX_DELAY = 2;
    }
}
