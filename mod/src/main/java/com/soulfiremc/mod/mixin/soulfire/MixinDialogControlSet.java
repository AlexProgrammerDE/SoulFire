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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.soulfiremc.mod.access.IDialogScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogControlSet;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.gui.screens.dialog.input.InputControlHandler;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.input.InputControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(DialogControlSet.class)
public class MixinDialogControlSet {
  @Shadow @Final private DialogScreen<?> screen;

  @WrapOperation(method = "addInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/dialog/input/InputControlHandlers;createHandler(Lnet/minecraft/server/dialog/input/InputControl;Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/screens/dialog/input/InputControlHandler$Output;)V"))
  private void captureInput(InputControl control, Screen targetScreen, InputControlHandler.Output output,
                            Operation<Void> original, Input input, Consumer<LayoutElement> layoutOutput) {
    original.call(control, targetScreen, (InputControlHandler.Output) (element, getter) -> {
      ((IDialogScreen) screen).soulfire$registerInput(input.key(), element, getter);
      output.accept(element, getter);
    });
  }

  @Inject(method = "createActionButton", at = @At("HEAD"))
  private void captureAction(ActionButton action, CallbackInfoReturnable<Button.Builder> cir) {
    ((IDialogScreen) screen).soulfire$registerAction(action);
  }
}
