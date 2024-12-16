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
package com.soulfiremc.server.protocol.bot.state;

import com.soulfiremc.server.data.EffectType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
public class EntityEffectState {
  private final Map<EffectType, EffectState> effects = new HashMap<>();

  public void updateEffect(
    EffectType effect,
    int amplifier,
    int duration,
    boolean ambient,
    boolean showParticles,
    boolean showIcon,
    boolean blend) {
    effects.put(
      effect,
      new EffectState(effect, amplifier, ambient, showParticles, showIcon, blend, duration));
  }

  public void removeEffect(EffectType effect) {
    effects.remove(effect);
  }

  public boolean hasEffect(EffectType effect) {
    return effects.containsKey(effect);
  }

  public Optional<EffectState> getEffect(EffectType effect) {
    return Optional.ofNullable(effects.get(effect));
  }

  public void tick() {
    effects.values().forEach(effect -> effect.duration(effect.duration() - 1));
  }

  @Getter
  @Setter
  @AllArgsConstructor
  public static class EffectState {
    private final EffectType effect;
    private final int amplifier;
    private final boolean ambient;
    private final boolean showParticles;
    private final boolean showIcon;
    private final boolean blend;
    private int duration;
  }
}
