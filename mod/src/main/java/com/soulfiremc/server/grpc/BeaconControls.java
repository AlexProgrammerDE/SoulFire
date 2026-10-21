/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.grpc;

import com.soulfiremc.mod.mixin.soulfire.BeaconPowerButtonAccessor;
import com.soulfiremc.mod.mixin.soulfire.BeaconScreenAccessor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class BeaconControls {
  private BeaconControls() {}

  static List<Control> describe(BeaconScreen screen) {
    var access = (BeaconScreenAccessor) screen;
    access.soulfire$updateButtons();
    var controls = new ArrayList<Control>();
    for (var button : access.soulfire$getButtons()) {
      if (!button.visible) continue;
      if (button instanceof BeaconPowerButtonAccessor power) {
        var effect = power.soulfire$getEffect();
        var primary = power.soulfire$isPrimary();
        var amplified = !primary && effect.equals(access.soulfire$getPrimary());
        var id = amplified ? -2 : BuiltInRegistries.MOB_EFFECT.getId(effect.value());
        var label = Component.translatable(effect.value().getDescriptionId());
        if (amplified) label.append(" II");
        var selected = Objects.equals(effect, primary ? access.soulfire$getPrimary() : access.soulfire$getSecondary());
        controls.add(new Control(id, label, selected, button));
      } else if (button.getMessage().equals(CommonComponents.GUI_DONE)) {
        controls.add(new Control(-1, button.getMessage(), false, button));
      } else if (button.getMessage().equals(CommonComponents.GUI_CANCEL)) {
        controls.add(new Control(-3, button.getMessage(), false, button));
      }
    }
    return List.copyOf(controls);
  }

  static boolean activate(BeaconScreen screen, int id) {
    var control = describe(screen).stream().filter(candidate -> candidate.id() == id).findFirst();
    if (control.isEmpty() || !control.get().button().active) return false;
    control.get().button().onPress(new KeyEvent(32, 0, 0));
    return true;
  }

  record Control(int id, Component label, boolean selected, AbstractButton button) {}
}
