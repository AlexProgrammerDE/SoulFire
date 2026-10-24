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
package com.soulfiremc.mod.mixin.headless.rendering;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/// Match single-line fields by keeping cursor movement and deletion on codepoint boundaries.
@Mixin(MultilineTextField.class)
public abstract class MixinMultilineTextField {
  @Shadow private String value;
  @Shadow private int cursor;

  @WrapMethod(method = "deleteText")
  private void deleteWholeCodepoint(int direction, Operation<Void> original) {
    // Word deletion already supplies an absolute UTF-16 distance.
    var distance = direction == -1 || direction == 1 ? Util.offsetByCodepoints(value, cursor, direction) - cursor : direction;
    original.call(distance);
  }

  @WrapMethod(method = "seekCursor")
  private void seekWholeCodepoints(Whence whence, int offset, Operation<Void> original) {
    original.call(whence, whence == Whence.RELATIVE ? Util.offsetByCodepoints(value, cursor, offset) - cursor : offset);
  }
}
