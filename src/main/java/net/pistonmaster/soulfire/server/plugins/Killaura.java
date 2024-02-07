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
import net.pistonmaster.soulfire.server.api.PluginHelper;
import net.pistonmaster.soulfire.server.api.SoulFireAPI;
import net.pistonmaster.soulfire.server.api.event.bot.BotPreTickEvent;
import net.pistonmaster.soulfire.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.soulfire.server.protocol.BotConnection;
import net.pistonmaster.soulfire.server.protocol.bot.BotActionManager;
import net.pistonmaster.soulfire.server.protocol.bot.state.entity.Entity;
import net.pistonmaster.soulfire.server.settings.lib.SettingsObject;
import net.pistonmaster.soulfire.server.settings.lib.property.BooleanProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.DoubleProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.Property;
import net.pistonmaster.soulfire.server.settings.lib.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Killaura implements InternalExtension {
    private static final Logger LOGGER = LoggerFactory.getLogger(Killaura.class);

    private static int tick = 0;

    public static void onPre(BotPreTickEvent event) {
        if (tick++ % 5 != 0) return;

        BotConnection bot = event.connection();
        //if (!bot.settingsHolder().get(KillauraSettings.ENABLE)) return;

        String whitelistedUser = bot.settingsHolder().get(KillauraSettings.WHITELISTED_USER);

        double lookRange = bot.settingsHolder().get(KillauraSettings.LOOK_RANGE);
        double hitRange = bot.settingsHolder().get(KillauraSettings.HIT_RANGE);
        double swingRange = bot.settingsHolder().get(KillauraSettings.SWING_RANGE);

        double max = Math.max(lookRange, Math.max(hitRange, swingRange));

        //Entity entity = bot.botControl()..getClosestEntity(bot, max, whitelistedUser, true);
        BotActionManager manager = bot.sessionDataManager().botActionManager();
        Entity entity = manager.getClosestEntity(max, whitelistedUser, true, true);
        if (entity == null) {
            //System.out.println("No entity found");
            return;
        }

        double distance = manager.distanceTo(entity);
        boolean swing = distance <= swingRange;
        if (distance <= lookRange) {
            manager.lookAt(entity);
        }

        if (distance <= hitRange) {
            manager.attack(entity, swing);
        } else if (swing) {
            manager.swingArm();
        }
    }

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(KillauraSettings.class, "KillAura");
    }

    @Override
    public void onLoad() {
        LOGGER.info("KillAura plugin is loading...");
        SoulFireAPI.registerListeners(Killaura.class);
        PluginHelper.registerBotEventConsumer(BotPreTickEvent.class, Killaura::onPre);
        LOGGER.info("KillAura plugin has been loaded!");
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private static class KillauraSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("plugin-killaura");
        public static final BooleanProperty ENABLE = BUILDER.ofBoolean( // TODO: 2/7/24 use this x)
                "enable",
                "Enable",
                new String[]{"--enable-killaura"},
                "Enable KillAura",
                false
        );

        public static final StringProperty WHITELISTED_USER = BUILDER.ofString(
                "whitelisted-user",
                "Whitelisted User",
                new String[]{"--killaura-whitelisted-user", "--killaura-whitelisted-username", "--kwu"},
                "This iser will be ignored by the KillAura",
                "Pansexuel"
        );

        public static final DoubleProperty HIT_RANGE = BUILDER.ofDouble(
                "hit-range",
                "Hit Range",
                new String[]{"--killaura-hit-range", "--killaura-hit-distance", "--khr"},
                "Distance for the killaura where the bot will start hitting the entity",
                3.0d,
                0.5d,
                6.0d,
                0.1d
        );
        public static final DoubleProperty SWING_RANGE = BUILDER.ofDouble(
                "swing-range",
                "Swing Range",
                new String[]{"--killaura-swing-range", "--killaura-swing-distance", "--ksr"},
                "Distance for the killaura where the bot will start swinging arm, set to 0 to disable",
                3.5d,
                0.0d,
                10.0d,
                0.0d
        );

        public static final DoubleProperty LOOK_RANGE = BUILDER.ofDouble(
                "look-range",
                "Look Range",
                new String[]{"--killaura-look-range", "--killaura-look-distance", "--klr"},
                "Distance for the killaura where the bot will start looking at the entity, set to 0 to disable",
                4.8d,
                0.0d,
                25.0d,
                0.0d
        );

        public static final BooleanProperty CHECK_WALLS = BUILDER.ofBoolean( // TODO: 2/7/24 use this too
                "check-walls",
                "Check Walls",
                new String[]{"--killaura-check-walls", "--killaura-cw"},
                "Check if the entity is behind a wall",
                true
        );

        public static final BooleanProperty IGNORE_COOLDOWN = BUILDER.ofBoolean( // TODO: 2/7/24 use this too
                "ignore-cooldown",
                "Ignore Cooldown",
                new String[]{"--killaura-ignore-cooldown", "--killaura-ic"},
                "Ignore the 1.9+ attack cooldown to act like a 1.8 killaura",
                false
        );
    }
}
