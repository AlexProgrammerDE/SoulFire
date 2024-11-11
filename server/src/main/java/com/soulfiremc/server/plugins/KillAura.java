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
package com.soulfiremc.server.plugins;

import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.BotPostEntityTickEvent;
import com.soulfiremc.server.api.event.bot.BotPreEntityTickEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.api.metadata.MetadataKey;
import com.soulfiremc.server.protocol.bot.state.entity.Entity;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.data.game.entity.RotationOrigin;

@Slf4j
public class KillAura extends InternalPlugin {
  private static final MetadataKey<AttackEntity> ATTACK_ENTITY = MetadataKey.of("kill_aura", "attack_entity", AttackEntity.class);

  public KillAura() {
    super(new PluginInfo(
      "kill-aura",
      "1.0.0",
      "Automatically attacks entities",
      "AlexProgrammerDE",
      "GPL-3.0"
    ));
  }

  @EventHandler
  public static void onPreEntityTick(BotPreEntityTickEvent event) {
    var bot = event.connection();
    if (!bot.settingsSource().get(KillAuraSettings.ENABLE)) {
      return;
    }

    var control = bot.botControl();

    var whitelistedUser = bot.settingsSource().get(KillAuraSettings.WHITELISTED_USER);

    var lookRange = bot.settingsSource().get(KillAuraSettings.LOOK_RANGE);
    var hitRange = bot.settingsSource().get(KillAuraSettings.HIT_RANGE);
    var swingRange = bot.settingsSource().get(KillAuraSettings.SWING_RANGE);

    var max = Math.max(lookRange, Math.max(hitRange, swingRange));

    var target =
      control.getClosestEntity(
        max,
        whitelistedUser,
        true,
        true,
        bot.settingsSource().get(KillAuraSettings.CHECK_WALLS));

    if (target == null) {
      return;
    }

    var bestVisiblePoint = control.getEntityVisiblePoint(target);
    var distance = bestVisiblePoint != null ?
      bestVisiblePoint.distance(bot.dataManager().clientEntity().eyePosition())
      : target.eyePosition().distance(bot.dataManager().clientEntity().eyePosition());

    if (distance > lookRange) {
      return;
    }

    if (bestVisiblePoint != null) {
      bot.dataManager().clientEntity().lookAt(RotationOrigin.EYES, bestVisiblePoint);
    } else {
      bot.dataManager()
        .clientEntity()
        .lookAt(RotationOrigin.EYES, target.originPosition(RotationOrigin.EYES));
    }

    bot.metadata().set(ATTACK_ENTITY, new AttackEntity(target, distance));
  }

  @EventHandler
  public static void onPostEntityTick(BotPostEntityTickEvent event) {
    var bot = event.connection();
    if (!bot.settingsSource().get(KillAuraSettings.ENABLE)) {
      return;
    }

    var control = bot.botControl();
    if (control.attackCooldownTicks() > 0) {
      return;
    }

    var attackEntity = bot.metadata().getAndRemove(ATTACK_ENTITY);
    if (attackEntity == null) {
      return;
    }

    var hitRange = bot.settingsSource().get(KillAuraSettings.HIT_RANGE);
    var swingRange = bot.settingsSource().get(KillAuraSettings.SWING_RANGE);
    var swing = attackEntity.distance() <= swingRange;
    if (attackEntity.distance() <= hitRange) {
      control.attack(attackEntity.attackEntity(), swing);
    } else if (swing) {
      control.swingArm();
    }

    if (bot.protocolVersion().olderThan(ProtocolVersion.v1_9)
      || bot.settingsSource().get(KillAuraSettings.IGNORE_COOLDOWN)) {
      control.attackCooldownTicks(bot.settingsSource().getRandom(KillAuraSettings.ATTACK_DELAY_TICKS).getAsInt());
    }
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(KillAuraSettings.class, "Kill Aura", this, "skull");
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class KillAuraSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("kill-aura");
    public static final BooleanProperty ENABLE =
      BUILDER.ofBoolean(
        "enable", "Enable", "Enable KillAura", false);
    public static final StringProperty WHITELISTED_USER =
      BUILDER.ofString(
        "whitelisted-user",
        "Whitelisted User",
        "This user will be ignored by the kill aura",
        "Pansexuel");
    public static final DoubleProperty HIT_RANGE =
      BUILDER.ofDouble(
        "hit-range",
        "Hit Range",
        "Range for the kill aura where the bot will start hitting the entity",
        3.0d,
        0.5d,
        6.0d,
        0.1d);
    public static final DoubleProperty SWING_RANGE =
      BUILDER.ofDouble(
        "swing-range",
        "Swing Range",
        "Range for the kill aura where the bot will start swinging arm, set to 0 to disable",
        3.5d,
        0.0d,
        10.0d,
        0.1d);
    public static final DoubleProperty LOOK_RANGE =
      BUILDER.ofDouble(
        "look-range",
        "Look Range",
        "Range for the kill aura where the bot will start looking at the entity, set to 0 to disable",
        4.8d,
        0.0d,
        25.0d,
        0.1d);
    public static final BooleanProperty CHECK_WALLS =
      BUILDER.ofBoolean(
        "check-walls",
        "Check Walls",
        "Check if the entity is behind a wall",
        true);
    public static final BooleanProperty IGNORE_COOLDOWN =
      BUILDER.ofBoolean(
        "ignore-cooldown",
        "Ignore Cooldown",
        "Ignore the 1.9+ attack cooldown to act like a 1.8 kill aura",
        false);
    public static final MinMaxPropertyLink ATTACK_DELAY_TICKS =
      new MinMaxPropertyLink(
        BUILDER.ofInt(
          "attack-delay-ticks-min",
          "Attack Delay Ticks Min",
          "Minimum tick delay between attacks on pre-1.9 versions",
          8,
          1,
          20,
          1),
        BUILDER.ofInt(
          "attack-delay-ticks-max",
          "Attack Delay Ticks Max",
          "Maximum tick delay between attacks on pre-1.9 versions",
          12,
          1,
          20,
          1));
  }

  private record AttackEntity(Entity attackEntity, double distance) {}
}
