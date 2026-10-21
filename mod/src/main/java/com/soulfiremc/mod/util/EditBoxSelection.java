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
package com.soulfiremc.mod.util;

import com.soulfiremc.mod.mixin.soulfire.EditBoxAccessor;
import net.minecraft.client.gui.components.EditBox;

/// Cursor and selection state that native screens omit when rebuilding their text widgets.
public record EditBoxSelection(int cursor, int anchor) {
  public static EditBoxSelection capture(EditBox box) {
    return new EditBoxSelection(box.getCursorPosition(), ((EditBoxAccessor) box).soulfire$getHighlightPosition());
  }

  public void restore(EditBox box) {
    box.setCursorPosition(cursor);
    box.setHighlightPos(anchor);
  }
}
