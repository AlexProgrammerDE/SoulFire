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
import com.soulfiremc.mod.util.EditBoxSelection;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/// POV resolution changes rebuild the title widget while the user is still editing it.
@Mixin(BookSignScreen.class)
public abstract class MixinBookSignScreen {
  @Shadow private EditBox titleBox;
  @Shadow private String titleValue;

  @WrapMethod(method = "init")
  private void preserveTitleDraft(Operation<Void> original) {
    var previous = titleBox;
    if (previous == null) {
      original.call();
      return;
    }
    titleValue = previous.getValue();
    var selection = EditBoxSelection.capture(previous);
    original.call();
    selection.restore(titleBox);
  }
}
