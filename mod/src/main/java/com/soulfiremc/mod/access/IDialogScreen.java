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
package com.soulfiremc.mod.access;

import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.action.Action;

import java.util.List;
import java.util.Map;

public interface IDialogScreen {
  String soulfire$getScreenId();
  Dialog soulfire$getDialog();
  Map<String, InputBinding> soulfire$getInputs();
  List<ActionButton> soulfire$getActions();
  void soulfire$registerInput(String key, LayoutElement element, Action.ValueGetter getter);
  void soulfire$registerAction(ActionButton action);

  record InputBinding(LayoutElement element, Action.ValueGetter getter) {}
}
