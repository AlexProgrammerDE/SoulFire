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
import com.soulfiremc.server.protocol.bot.ControllingTask;
import com.soulfiremc.server.protocol.bot.state.entity.Entity;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.data.game.entity.RotationOrigin;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.pf4j.Extension;

@Slf4j
@Extension
public class KillAura extends InternalPlugin {
  private static final MetadataKey<Integer> COOLDOWN = MetadataKey.of("kill_aura", "cooldown", Integer.class);

  public KillAura() {
    super(new PluginInfo(
      "kill-aura",
      "1.0.0",
      "Automatically attacks entities",
      "AlexProgrammerDE",
      "GPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  @EventHandler
  public static void onPreEntityTick(BotPreEntityTickEvent event) {
    var bot = event.connection();
    if (!bot.settingsSource().get(KillAuraSettings.ENABLE)) {
      return;
    }

    var control = bot.botControl();
    var localPlayer = bot.dataManager().localPlayer();
    if (control.activelyControlled()) {
      return;
    }

    var whitelistedUsers = bot.settingsSource().get(KillAuraSettings.WHITELISTED_USERS);

    var lookRange = bot.settingsSource().get(KillAuraSettings.LOOK_RANGE);
    var hitRange = bot.settingsSource().get(KillAuraSettings.HIT_RANGE);
    var swingRange = bot.settingsSource().get(KillAuraSettings.SWING_RANGE);

    var max = Math.max(lookRange, Math.max(hitRange, swingRange));

    var target =
      control.getClosestEntity(
        max,
        whitelistedUsers,
        true,
        true,
        bot.settingsSource().get(KillAuraSettings.CHECK_WALLS));

    if (target == null) {
      return;
    }

    var bestVisiblePoint = control.getEntityVisiblePoint(target);
    if (bestVisiblePoint == null) {
      bestVisiblePoint = target.originPosition(RotationOrigin.EYES);
    }

    var distance = bestVisiblePoint.distance(localPlayer.eyePosition());

    if (distance > lookRange) {
      return;
    }

    control.registerControllingTask(ControllingTask.manual(new KillAuraMarker(target, distance)));
    localPlayer.lookAt(RotationOrigin.EYES, bestVisiblePoint);
  }

  @EventHandler
  public static void onPostEntityTick(BotPostEntityTickEvent event) {
    var bot = event.connection();
    var localPlayer = bot.dataManager().localPlayer();
    var control = bot.botControl();
    if (localPlayer.getAttackStrengthScale(0) != 1F) {
      return;
    }

    int cooldownTicks = bot.metadata().getOrDefault(COOLDOWN, 0);
    if (cooldownTicks > 0) {
      bot.metadata().set(COOLDOWN, cooldownTicks - 1);
      return;
    }

    var marker = control.getMarkerAndUnregister(KillAuraMarker.class);
    if (marker == null) {
      return;
    }

    var hitRange = bot.settingsSource().get(KillAuraSettings.HIT_RANGE);
    var swingRange = bot.settingsSource().get(KillAuraSettings.SWING_RANGE);
    var swing = marker.distance() <= swingRange;
    if (marker.distance() <= hitRange) {
      bot.dataManager().gameModeState().attack(localPlayer, marker.attackEntity());
      localPlayer.swing(Hand.MAIN_HAND);
    } else if (swing) {
      localPlayer.swing(Hand.MAIN_HAND);
    }

    // Custom attack delay for specific scenarios
    if (bot.protocolVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)
      || bot.settingsSource().get(KillAuraSettings.IGNORE_COOLDOWN)) {
      bot.metadata().set(COOLDOWN, bot.settingsSource().getRandom(KillAuraSettings.ATTACK_DELAY_TICKS).getAsInt());
    }
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addPluginPage(KillAuraSettings.class, "Kill Aura", this, "skull", KillAuraSettings.ENABLE);
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class KillAuraSettings implements SettingsObject {
    private static final String NAMESPACE = "kill-aura";
    public static final BooleanProperty ENABLE =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enable")
        .uiName("Enable")
        .description("Enable KillAura")
        .defaultValue(false)
        .build();
    public static final StringListProperty WHITELISTED_USERS =
      ImmutableStringListProperty.builder()
        .namespace(NAMESPACE)
        .key("whitelisted-users")
        .uiName("Whitelisted Users")
        .description("These users will be ignored by the kill aura")
        .addDefaultValue("Dinnerbone")
        .build();
    public static final DoubleProperty HIT_RANGE =
      ImmutableDoubleProperty.builder()
        .namespace(NAMESPACE)
        .key("hit-range")
        .uiName("Hit Range")
        .description("Range for the kill aura where the bot will start hitting the entity")
        .defaultValue(3.0d)
        .minValue(0.5d)
        .maxValue(6.0d)
        .stepValue(0.1d)
        .build();
    public static final DoubleProperty SWING_RANGE =
      ImmutableDoubleProperty.builder()
        .namespace(NAMESPACE)
        .key("swing-range")
        .uiName("Swing Range")
        .description("Range for the kill aura where the bot will start swinging arm, set to 0 to disable")
        .defaultValue(3.5d)
        .minValue(0.0d)
        .maxValue(10.0d)
        .stepValue(0.1d)
        .build();
    public static final DoubleProperty LOOK_RANGE =
      ImmutableDoubleProperty.builder()
        .namespace(NAMESPACE)
        .key("look-range")
        .uiName("Look Range")
        .description("Range for the kill aura where the bot will start looking at the entity, set to 0 to disable")
        .defaultValue(4.8d)
        .minValue(0.0d)
        .maxValue(25.0d)
        .stepValue(0.1d)
        .build();
    public static final BooleanProperty CHECK_WALLS =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("check-walls")
        .uiName("Check Walls")
        .description("Check if the entity is behind a wall")
        .defaultValue(true)
        .build();
    public static final BooleanProperty IGNORE_COOLDOWN =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("ignore-cooldown")
        .uiName("Ignore Cooldown")
        .description("Ignore the 1.9+ attack cooldown to act like a 1.8 kill aura")
        .defaultValue(false)
        .build();
    public static final MinMaxProperty ATTACK_DELAY_TICKS =
      ImmutableMinMaxProperty.builder()
        .namespace(NAMESPACE)
        .key("attack-delay-ticks")
        .minValue(1)
        .maxValue(20)
        .minEntry(ImmutableMinMaxPropertyEntry.builder()
          .uiName("Attack Delay Ticks Min")
          .description("Minimum tick delay between attacks on pre-1.9 versions")
          .defaultValue(8)
          .build())
        .maxEntry(ImmutableMinMaxPropertyEntry.builder()
          .uiName("Attack Delay Ticks Max")
          .description("Maximum tick delay between attacks on pre-1.9 versions")
          .defaultValue(12)
          .build())
        .build();
  }

  private record KillAuraMarker(Entity attackEntity, double distance) implements ControllingTask.ManualTaskMarker {
  }
}
