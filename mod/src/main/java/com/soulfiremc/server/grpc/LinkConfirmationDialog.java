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

import com.mojang.blaze3d.platform.InputConstants;
import com.soulfiremc.grpc.generated.DialogAfterAction;
import com.soulfiremc.grpc.generated.DialogBodyElement;
import com.soulfiremc.grpc.generated.DialogButton;
import com.soulfiremc.grpc.generated.DialogPlainMessage;
import com.soulfiremc.grpc.generated.DialogType;
import com.soulfiremc.grpc.generated.ServerDialog;
import com.soulfiremc.mod.access.IScreenIdentity;
import io.grpc.Status;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.input.KeyEvent;

import java.util.List;
import java.util.Map;

/// Exposes Minecraft's own URL confirmation widgets and callbacks to the dialog API.
final class LinkConfirmationDialog {
  private LinkConfirmationDialog() {}

  static ServerDialog describe(ConfirmLinkScreen screen) {
    var buttons = buttons(screen);
    var result = ServerDialog.newBuilder()
      .setScreenId(((IScreenIdentity) screen).soulfire$getScreenId())
      .setType(DialogType.DIALOG_TYPE_CONFIRMATION)
      .setTitle(screen.getTitle().getString())
      .setPause(screen.isPauseScreen())
      .setCanCloseWithEscape(canDismiss(buttons))
      .setAfterAction(DialogAfterAction.DIALOG_AFTER_ACTION_CLOSE);
    for (var child : screen.children()) {
      if (child instanceof AbstractStringWidget text && !text.getMessage().equals(screen.getTitle())) {
        result.addBody(DialogBodyElement.newBuilder().setPlainMessage(DialogPlainMessage.newBuilder()
          .setContents(text.getMessage().getString()).setWidth(text.getWidth())));
      }
    }
    for (var button : buttons) result.addActions(DialogButton.newBuilder().setLabel(button.getMessage().getString()).setWidth(button.getWidth()));
    if (!buttons.isEmpty()) {
      result.setYes(result.getActions(0));
      result.setNo(result.getActions(buttons.size() - 1));
      result.setExitAction(result.getActions(buttons.size() - 1));
    }
    return result.build();
  }

  static DialogControls.ClientActions act(ConfirmLinkScreen screen, Map<String, String> values, int index) {
    if (!values.isEmpty()) throw Status.INVALID_ARGUMENT.withDescription("Link confirmations do not have inputs").asRuntimeException();
    var buttons = buttons(screen);
    if (index < -1 || index >= buttons.size()) throw Status.INVALID_ARGUMENT.withDescription("Invalid dialog button index").asRuntimeException();
    if (index == -1) {
      if (!canDismiss(buttons)) throw Status.FAILED_PRECONDITION.withDescription("This confirmation cannot be dismissed yet").asRuntimeException();
      return DialogControls.captureClientActions(() -> screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 27, 0)));
    }
    var button = buttons.get(index);
    if (!button.active || !button.visible) throw Status.FAILED_PRECONDITION.withDescription("This dialog action is not available yet").asRuntimeException();
    return DialogControls.captureClientActions(() -> button.onPress(new KeyEvent(InputConstants.KEY_RETURN, 13, 0)));
  }

  private static List<Button> buttons(ConfirmLinkScreen screen) {
    return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
  }

  private static boolean canDismiss(List<Button> buttons) {
    // ConfirmScreen delays both confirmation and cancellation together.
    return !buttons.isEmpty() && buttons.getFirst().active && buttons.getLast().active;
  }
}
