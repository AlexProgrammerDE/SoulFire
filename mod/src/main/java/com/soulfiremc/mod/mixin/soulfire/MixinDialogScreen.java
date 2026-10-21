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
package com.soulfiremc.mod.mixin.soulfire;

import com.soulfiremc.mod.access.IDialogScreen;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.action.Action;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(DialogScreen.class)
public abstract class MixinDialogScreen implements IDialogScreen {
  @Shadow @Final private Dialog dialog;
  @Unique private final String soulfire$screenId = UUID.randomUUID().toString();
  @Unique private final Map<String, InputBinding> soulfire$inputs = new LinkedHashMap<>();
  @Unique private final List<ActionButton> soulfire$actions = new ArrayList<>();

  @Inject(method = "init", at = @At("HEAD"))
  private void resetControls(CallbackInfo ci) {
    soulfire$inputs.clear();
    soulfire$actions.clear();
  }

  @Override public String soulfire$getScreenId() { return soulfire$screenId; }
  @Override public Dialog soulfire$getDialog() { return dialog; }
  @Override public Map<String, InputBinding> soulfire$getInputs() { return Map.copyOf(soulfire$inputs); }
  @Override public List<ActionButton> soulfire$getActions() { return List.copyOf(soulfire$actions); }
  @Override public void soulfire$registerInput(String key, LayoutElement element, Action.ValueGetter getter) {
    soulfire$inputs.put(key, new InputBinding(element, getter));
  }
  @Override public void soulfire$registerAction(ActionButton action) { soulfire$actions.add(action); }
}
