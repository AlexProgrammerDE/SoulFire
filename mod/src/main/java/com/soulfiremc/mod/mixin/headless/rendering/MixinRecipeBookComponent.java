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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RecipeBookComponent.class)
public abstract class MixinRecipeBookComponent {
  @Shadow private @Nullable EditBox searchBox;

  @WrapMethod(method = "init")
  private void preserveSearch(int width, int height, Minecraft minecraft, boolean widthTooNarrow, Operation<Void> original) {
    var previous = searchBox;
    if (previous == null) {
      original.call(width, height, minecraft, widthTooNarrow);
      return;
    }
    var selection = EditBoxSelection.capture(previous);
    var focused = previous.isFocused();
    original.call(width, height, minecraft, widthTooNarrow);
    if (searchBox != null) {
      selection.restore(searchBox);
      searchBox.setFocused(focused);
    }
  }
}
