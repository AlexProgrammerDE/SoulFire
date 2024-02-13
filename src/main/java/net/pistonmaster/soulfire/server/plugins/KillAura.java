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

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.soulfire.server.api.BotContext;
import net.pistonmaster.soulfire.server.api.PluginHelper;
import net.pistonmaster.soulfire.server.api.SoulFireAPI;
import net.pistonmaster.soulfire.server.api.event.bot.BotPostTickEvent;
import net.pistonmaster.soulfire.server.api.event.bot.BotPreTickEvent;
import net.pistonmaster.soulfire.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.soulfire.server.protocol.bot.state.entity.Entity;
import net.pistonmaster.soulfire.server.settings.lib.SettingsObject;
import net.pistonmaster.soulfire.server.settings.lib.property.BooleanProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.DoubleProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.Property;
import net.pistonmaster.soulfire.server.settings.lib.property.StringProperty;
import net.raphimc.vialoader.util.VersionEnum;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class KillAura implements InternalExtension {
    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(KillAuraSettings.class, "Kill Aura");
    }

    @Override
    public void onLoad() {
        SoulFireAPI.registerListeners(KillAura.class);
        PluginHelper.registerBotContextFactory(connection -> new BotContext() {
            @EventHandler
            public void onPre(BotPreTickEvent event) {
                var bot = event.connection();
                if (!bot.settingsHolder().get(KillAuraSettings.ENABLE)) return;

                var control = bot.botControl();

                var whitelistedUser = bot.settingsHolder().get(KillAuraSettings.WHITELISTED_USER);

                var lookRange = bot.settingsHolder().get(KillAuraSettings.LOOK_RANGE);
                var hitRange = bot.settingsHolder().get(KillAuraSettings.HIT_RANGE);
                var swingRange = bot.settingsHolder().get(KillAuraSettings.SWING_RANGE);

                var max = Math.max(lookRange, Math.max(hitRange, swingRange));

                var entity = control.getClosestEntity(max, whitelistedUser, true, true, bot.settingsHolder().get(KillAuraSettings.CHECK_WALLS));
                if (entity == null) return;

                control.extraData().put("kill_aura_target", entity);

                double distance;
                var bestVisiblePoint = control.getEntityVisiblePoint(entity);
                if (bestVisiblePoint != null) {
                    distance = bestVisiblePoint.distance(
                            bot.sessionDataManager().clientEntity().getEyePosition()
                    );
                } else {
                    distance = entity.getEyePosition().distance(
                            bot.sessionDataManager().clientEntity().getEyePosition()
                    );
                }

                if (distance > lookRange) {
                    return;
                }

                if (bestVisiblePoint != null) {
                    bot.sessionDataManager().clientEntity().lookAt(RotationOrigin.EYES, bestVisiblePoint);
                } else {
                    bot.sessionDataManager().clientEntity().lookAt(RotationOrigin.EYES, RotationOrigin.EYES, entity);
                }

                control.extraData().put("kill_aura_looked_at", true);
            }

            @EventHandler
            public void onPost(BotPostTickEvent event) {
                var bot = event.connection();
                if (!bot.settingsHolder().get(KillAuraSettings.ENABLE)) return;

                var control = bot.botControl();

                var currentTime = System.currentTimeMillis();
                var nextHit = (long) control.extraData().computeIfAbsent("next_hit", k -> currentTime);
                if (nextHit > currentTime) {
                    return;
                }

                var lookRange = bot.settingsHolder().get(KillAuraSettings.LOOK_RANGE);
                var hitRange = bot.settingsHolder().get(KillAuraSettings.HIT_RANGE);
                var swingRange = bot.settingsHolder().get(KillAuraSettings.SWING_RANGE);

                var target = (Entity) control.extraData().get("kill_aura_target");
                if (target != null) {
                    control.extraData().remove("kill_aura_target");
                }
                if (target == null) return;

                var lookedAt = (Boolean) control.extraData().get("kill_aura_looked_at");
                if (lookedAt != null) {
                    control.extraData().remove("kill_aura_looked_at");
                } else {
                    lookedAt = false;
                }

                if (!lookedAt && lookRange != 0) return;

                double distance;
                var bestVisiblePoint = control.getEntityVisiblePoint(target);
                if (bestVisiblePoint != null) {
                    distance = bestVisiblePoint.distance(
                            bot.sessionDataManager().clientEntity().getEyePosition()
                    );
                } else {
                    distance = target.getEyePosition().distance(
                            bot.sessionDataManager().clientEntity().getEyePosition()
                    );
                }

                var swing = distance <= swingRange;
                if (distance <= hitRange) {
                    control.attack(target, swing);
                } else if (swing) {
                    control.swingArm();
                }

                if (bot.meta().versionEnum().isOlderThan(VersionEnum.r1_9)
                        || bot.settingsHolder().get(KillAuraSettings.IGNORE_COOLDOWN)) {
                    var cpsMin = bot.settingsHolder().get(KillAuraSettings.CPS_MIN);
                    var cpsMax = bot.settingsHolder().get(KillAuraSettings.CPS_MAX);
                    var randomDelay = 1000.0d / ThreadLocalRandom.current().nextDouble(cpsMin, cpsMax);
                    // control.extraData().put("next_hit", control.lastHit() + randomDelay);
                } else {
                    control.extraData().put("next_hit", control.getHitItemCooldownTicks());
                }
            }
        });
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private static class KillAuraSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("kill-aura");
        public static final BooleanProperty ENABLE = BUILDER.ofBoolean(
                "enable",
                "Enable",
                new String[]{"--kill-aura"},
                "Enable KillAura",
                false
        );

        public static final StringProperty WHITELISTED_USER = BUILDER.ofString(
                "whitelisted-user",
                "Whitelisted User",
                new String[]{"--kill-aura-whitelisted-user", "--kill-aura-whitelisted-username", "--kwu"},
                "This user will be ignored by the kill aura",
                "Pansexuel"
        );

        public static final DoubleProperty HIT_RANGE = BUILDER.ofDouble(
                "hit-range",
                "Hit Range",
                new String[]{"--kill-aura-hit-range", "--kill-aura-hit-distance", "--khr"},
                "Distance for the kill aura where the bot will start hitting the entity",
                3.0d,
                0.5d,
                6.0d,
                0.1d
        );
        public static final DoubleProperty SWING_RANGE = BUILDER.ofDouble(
                "swing-range",
                "Swing Range",
                new String[]{"--kill-aura-swing-range", "--kill-aura-swing-distance", "--ksr"},
                "Distance for the kill aura where the bot will start swinging arm, set to 0 to disable",
                3.5d,
                0.0d,
                10.0d,
                0.0d
        );

        public static final DoubleProperty LOOK_RANGE = BUILDER.ofDouble(
                "look-range",
                "Look Range",
                new String[]{"--kill-aura-look-range", "--kill-aura-look-distance", "--klr"},
                "Distance for the kill aura where the bot will start looking at the entity, set to 0 to disable",
                4.8d,
                0.0d,
                25.0d,
                0.0d
        );

        public static final BooleanProperty CHECK_WALLS = BUILDER.ofBoolean(
                "check-walls",
                "Check Walls",
                new String[]{"--kill-aura-check-walls", "--kill-aura-cw"},
                "Check if the entity is behind a wall",
                true
        );

        public static final BooleanProperty IGNORE_COOLDOWN = BUILDER.ofBoolean(
                "ignore-cooldown",
                "Ignore Cooldown",
                new String[]{"--kill-aura-ignore-cooldown", "--kill-aura-ic"},
                "Ignore the 1.9+ attack cooldown to act like a 1.8 kill aura",
                false
        );

        public static final DoubleProperty CPS_MIN = BUILDER.ofDouble(
                "cps-min",
                "CPS Min",
                new String[]{"--kill-aura-cps-min"},
                "Minimum CPS for the kill aura",
                8.0d,
                0.1d,
                20.0d,
                0.1d
        );

        public static final DoubleProperty CPS_MAX = BUILDER.ofDouble(
                "cps-max",
                "CPS Max",
                new String[]{"--kill-aura-cps-max"},
                "Maximum CPS for the kill aura",
                12.0d,
                0.1d,
                20.0d,
                0.1d
        );
    }
}