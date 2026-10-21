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
import com.soulfiremc.mod.mixin.soulfire.MultiLineEditBoxAccessor;
import com.soulfiremc.mod.mixin.soulfire.MultilineTextFieldAccessor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/// Keep page editing at the same selection when POV resizing rebuilds the widget.
@Mixin(BookEditScreen.class)
public abstract class MixinBookEditScreen {
  @Shadow private MultiLineEditBox page;

  @WrapMethod(method = "init")
  private void preservePageSelection(Operation<Void> original) {
    if (page == null) {
      original.call();
      return;
    }
    var previous = ((MultiLineEditBoxAccessor) page).soulfire$getTextField();
    var cursor = previous.cursor();
    var anchor = ((MultilineTextFieldAccessor) previous).soulfire$getSelectionAnchor();
    original.call();
    var current = ((MultiLineEditBoxAccessor) page).soulfire$getTextField();
    current.seekCursor(Whence.ABSOLUTE, anchor);
    current.setSelecting(true);
    current.seekCursor(Whence.ABSOLUTE, cursor);
    current.setSelecting(false);
  }
}
