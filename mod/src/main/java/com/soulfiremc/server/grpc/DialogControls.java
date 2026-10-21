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

import com.soulfiremc.mod.access.IDialogScreen;
import com.soulfiremc.server.renderer.PovClientActions;
import io.grpc.Status;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.server.dialog.input.BooleanInput;
import net.minecraft.server.dialog.input.InputControl;
import net.minecraft.server.dialog.input.NumberRangeInput;
import net.minecraft.server.dialog.input.SingleOptionInput;
import net.minecraft.server.dialog.input.TextInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

final class DialogControls {
  private DialogControls() {}

  static ClientActions act(DialogScreen<?> screen, Map<String, String> values, int index) {
    var controls = (IDialogScreen) screen;
    var actions = controls.soulfire$getActions();
    if (index < -1 || index >= actions.size()) throw invalid("Invalid dialog button index");
    if (index == -1 && !screen.shouldCloseOnEsc()) throw invalid("This dialog cannot be dismissed");

    var bindings = controls.soulfire$getInputs();
    var definitions = new HashMap<String, InputControl>();
    for (var input : controls.soulfire$getDialog().common().inputs()) definitions.put(input.key(), input.control());
    var updates = new ArrayList<Runnable>();
    for (var entry : values.entrySet()) {
      var definition = definitions.get(entry.getKey());
      var binding = bindings.get(entry.getKey());
      if (definition == null || binding == null) throw invalid("Unknown dialog input: " + entry.getKey());
      updates.add(prepare(definition, binding, entry.getValue()));
    }
    // Validate every value before modifying any native widget.
    updates.forEach(Runnable::run);
    var clipboard = new AtomicReference<String>();
    var url = new AtomicReference<String>();
    ScopedValue.where(PovClientActions.CURRENT, new PovClientActions(clipboard::set, url::set)).run(() -> {
      if (index == -1) {
        screen.onClose();
      } else {
        var getters = new HashMap<String, net.minecraft.server.dialog.action.Action.ValueGetter>();
        bindings.forEach((key, binding) -> getters.put(key, binding.getter()));
        screen.runAction(actions.get(index).action().flatMap(action -> action.createAction(getters)));
      }
    });
    return new ClientActions(Optional.ofNullable(clipboard.get()), Optional.ofNullable(url.get()));
  }

  private static Runnable prepare(InputControl input, IDialogScreen.InputBinding binding, String value) {
    return switch (input) {
      case TextInput text -> {
        if (value.length() > text.maxLength()) throw invalid("Dialog text exceeds its maximum length");
        if (text.multiline().isPresent()) {
          var widget = widget(binding, MultiLineEditBox.class);
          yield () -> widget.setValue(value);
        }
        var widget = widget(binding, EditBox.class);
        yield () -> widget.setValue(value);
      }
      case BooleanInput ignored -> {
        if (!value.equals("true") && !value.equals("false")) throw invalid("Dialog checkbox requires true or false");
        var selected = Boolean.parseBoolean(value);
        var widget = widget(binding, Checkbox.class);
        yield () -> { if (widget.selected() != selected) widget.onPress(new KeyEvent(32, 0, 0)); };
      }
      case SingleOptionInput option -> {
        var selected = option.entries().stream().filter(entry -> entry.id().equals(value)).findFirst()
          .orElseThrow(() -> invalid("Unknown dialog option"));
        @SuppressWarnings("unchecked")
        var widget = (CycleButton<SingleOptionInput.Entry>) widget(binding, CycleButton.class);
        yield () -> widget.setValue(selected);
      }
      case NumberRangeInput range -> {
        float number;
        try { number = Float.parseFloat(value); } catch (NumberFormatException e) { throw invalid("Dialog range requires a number"); }
        var info = range.rangeInfo();
        if (!Float.isFinite(number) || number < Math.min(info.start(), info.end()) || number > Math.max(info.start(), info.end())) {
          throw invalid("Dialog range value is outside its bounds");
        }
        var fraction = info.start() == info.end() ? 0.5 : (number - info.start()) / (double) (info.end() - info.start());
        var widget = widget(binding, AbstractSliderButton.class);
        yield () -> widget.setValue(fraction);
      }
      default -> throw invalid("Unsupported dialog input type");
    };
  }

  private static <T extends AbstractWidget> T widget(IDialogScreen.InputBinding binding, Class<T> type) {
    var widgets = new ArrayList<AbstractWidget>();
    binding.element().visitWidgets(widgets::add);
    return widgets.stream().filter(type::isInstance).map(type::cast).findFirst()
      .orElseThrow(() -> Status.FAILED_PRECONDITION.withDescription("Dialog input widget is not available").asRuntimeException());
  }

  private static RuntimeException invalid(String message) { return Status.INVALID_ARGUMENT.withDescription(message).asRuntimeException(); }
  record ClientActions(Optional<String> clipboard, Optional<String> openUrl) {}
}
