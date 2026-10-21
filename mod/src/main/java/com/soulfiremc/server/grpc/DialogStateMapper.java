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

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.soulfiremc.grpc.generated.DialogAction;
import com.soulfiremc.grpc.generated.DialogAfterAction;
import com.soulfiremc.grpc.generated.DialogBodyElement;
import com.soulfiremc.grpc.generated.DialogBooleanInput;
import com.soulfiremc.grpc.generated.DialogButton;
import com.soulfiremc.grpc.generated.DialogCopyToClipboardAction;
import com.soulfiremc.grpc.generated.DialogCustomAction;
import com.soulfiremc.grpc.generated.DialogDynamicCustomAction;
import com.soulfiremc.grpc.generated.DialogDynamicRunCommandAction;
import com.soulfiremc.grpc.generated.DialogInput;
import com.soulfiremc.grpc.generated.DialogItem;
import com.soulfiremc.grpc.generated.DialogNumberRangeInput;
import com.soulfiremc.grpc.generated.DialogOpenUrlAction;
import com.soulfiremc.grpc.generated.DialogOption;
import com.soulfiremc.grpc.generated.DialogPlainMessage;
import com.soulfiremc.grpc.generated.DialogRunCommandAction;
import com.soulfiremc.grpc.generated.DialogShowDialogAction;
import com.soulfiremc.grpc.generated.DialogSingleOptionInput;
import com.soulfiremc.grpc.generated.DialogSuggestCommandAction;
import com.soulfiremc.grpc.generated.DialogTextInput;
import com.soulfiremc.grpc.generated.DialogType;
import com.soulfiremc.grpc.generated.ServerDialog;
import com.soulfiremc.mod.access.IDialogScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.dialog.ButtonListDialog;
import net.minecraft.server.dialog.ConfirmationDialog;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogListDialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.NoticeDialog;
import net.minecraft.server.dialog.ServerLinksDialog;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.BooleanInput;
import net.minecraft.server.dialog.input.NumberRangeInput;
import net.minecraft.server.dialog.input.SingleOptionInput;
import net.minecraft.server.dialog.input.TextInput;

import java.util.Optional;

final class DialogStateMapper {
  private DialogStateMapper() {}

  static ServerDialog describe(Holder<Dialog> holder, IDialogScreen screen, ClientLevel level) {
    var dialog = holder.value();
    var common = dialog.common();
    var result = ServerDialog.newBuilder().setScreenId(screen.soulfire$getScreenId()).setTitle(common.title().getString())
      .setCanCloseWithEscape(common.canCloseWithEscape()).setPause(common.pause())
      .setAfterAction(switch (common.afterAction()) {
        case CLOSE -> DialogAfterAction.DIALOG_AFTER_ACTION_CLOSE;
        case NONE -> DialogAfterAction.DIALOG_AFTER_ACTION_NONE;
        case WAIT_FOR_RESPONSE -> DialogAfterAction.DIALOG_AFTER_ACTION_WAIT_FOR_RESPONSE;
      });
    holder.unwrapKey().ifPresent(key -> result.setId(key.identifier().toString()));
    common.externalTitle().ifPresent(title -> result.setExternalTitle(title.getString()));
    result.setType(switch (dialog) {
      case NoticeDialog ignored -> DialogType.DIALOG_TYPE_NOTICE;
      case ConfirmationDialog ignored -> DialogType.DIALOG_TYPE_CONFIRMATION;
      case MultiActionDialog ignored -> DialogType.DIALOG_TYPE_MULTI_ACTION;
      case ServerLinksDialog ignored -> DialogType.DIALOG_TYPE_SERVER_LINKS;
      case DialogListDialog ignored -> DialogType.DIALOG_TYPE_DIALOG_LIST;
      default -> DialogType.DIALOG_TYPE_UNSPECIFIED;
    });
    for (var body : common.body()) {
      var element = DialogBodyElement.newBuilder();
      switch (body) {
        case PlainMessage text -> element.setPlainMessage(DialogPlainMessage.newBuilder().setContents(text.contents().getString()).setWidth(text.width()));
        case ItemBody item -> {
          var display = DialogItem.newBuilder().setItemId(item.item().item().getRegisteredName()).setCount(item.item().count())
            .setShowDecoration(item.showDecorations()).setShowTooltip(item.showTooltip()).setWidth(item.width()).setHeight(item.height());
          item.description().ifPresent(description -> display.setDescription(description.contents().getString()));
          element.setItem(display);
        }
        default -> { continue; }
      }
      result.addBody(element);
    }
    var bindings = screen.soulfire$getInputs();
    for (var input : common.inputs()) {
      var binding = bindings.get(input.key());
      if (binding == null) continue;
      var current = binding.getter().asTag();
      var value = current instanceof StringTag text ? text.value() : binding.getter().asTemplateSubstitution();
      var element = DialogInput.newBuilder();
      switch (input.control()) {
        case TextInput text -> {
          var control = DialogTextInput.newBuilder().setKey(input.key()).setLabel(text.label().getString()).setWidth(text.width())
            .setLabelVisible(text.labelVisible()).setInitial(value).setMaxLength(text.maxLength()).setMultiline(text.multiline().isPresent());
          text.multiline().ifPresent(multiline -> {
            multiline.maxLines().ifPresent(control::setMultilineMaxLines);
            multiline.height().ifPresent(control::setMultilineHeight);
          });
          element.setText(control);
        }
        case BooleanInput checkbox -> element.setBoolean(DialogBooleanInput.newBuilder().setKey(input.key()).setLabel(checkbox.label().getString())
          .setInitial(current instanceof NumericTag number && number.byteValue() != 0).setOnTrue(checkbox.onTrue()).setOnFalse(checkbox.onFalse()));
        case SingleOptionInput option -> {
          var control = DialogSingleOptionInput.newBuilder().setKey(input.key()).setLabel(option.label().getString()).setWidth(option.width())
            .setLabelVisible(option.labelVisible()).setInitialOptionId(value);
          for (var entry : option.entries()) control.addOptions(DialogOption.newBuilder().setId(entry.id()).setDisplay(entry.displayOrDefault().getString()));
          element.setSingleOption(control);
        }
        case NumberRangeInput range -> {
          var info = range.rangeInfo();
          element.setNumberRange(DialogNumberRangeInput.newBuilder().setKey(input.key()).setLabel(range.label().getString()).setWidth(range.width())
            .setLabelFormat(range.labelFormat()).setStart(info.start()).setEnd(info.end()).setStep(info.step().orElse(0f))
            .setInitial(current instanceof NumericTag number ? number.doubleValue() : 0));
        }
        default -> { continue; }
      }
      result.addInputs(element);
    }
    for (var action : screen.soulfire$getActions()) {
      var button = DialogButton.newBuilder().setLabel(action.button().label().getString()).setWidth(action.button().width());
      action.button().tooltip().ifPresent(tooltip -> button.setTooltip(tooltip.getString()));
      action.action().flatMap(value -> describeAction(value, level)).ifPresent(button::setAction);
      result.addActions(button);
    }
    if (dialog instanceof NoticeDialog && result.getActionsCount() > 0) result.setAction(result.getActions(0));
    if (dialog instanceof ConfirmationDialog && result.getActionsCount() > 1) {
      result.setYes(result.getActions(0)); result.setNo(result.getActions(1));
    }
    if (dialog instanceof ButtonListDialog list) {
      result.setColumns(list.columns());
      if (list.exitAction().isPresent() && result.getActionsCount() > 0) result.setExitAction(result.getActions(result.getActionsCount() - 1));
    }
    if (dialog instanceof DialogListDialog list) result.setButtonWidth(list.buttonWidth());
    if (dialog instanceof ServerLinksDialog links) result.setButtonWidth(links.buttonWidth());
    return result.build();
  }

  private static Optional<DialogAction> describeAction(Action action, ClientLevel level) {
    var json = Action.CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), action).getOrThrow().getAsJsonObject();
    var type = string(json, "type").replace("minecraft:", "");
    var result = DialogAction.newBuilder();
    switch (type) {
      case "open_url" -> result.setOpenUrl(DialogOpenUrlAction.newBuilder().setUrl(string(json, "url")));
      case "run_command" -> result.setRunCommand(DialogRunCommandAction.newBuilder().setCommand(string(json, "command")));
      case "suggest_command" -> result.setSuggestCommand(DialogSuggestCommandAction.newBuilder().setCommand(string(json, "command")));
      case "copy_to_clipboard" -> result.setCopyToClipboard(DialogCopyToClipboardAction.newBuilder().setValue(string(json, "value")));
      case "show_dialog" -> {
        var target = json.get("dialog");
        if (target == null || !target.isJsonPrimitive()) return Optional.empty();
        result.setShowDialog(DialogShowDialogAction.newBuilder().setDialogId(target.getAsString()));
      }
      case "custom" -> result.setCustom(DialogCustomAction.newBuilder().setId(string(json, "id")).setPayload(json.has("payload") ? json.get("payload").toString() : ""));
      case "dynamic/run_command" -> result.setDynamicRunCommand(DialogDynamicRunCommandAction.newBuilder().setTemplate(string(json, "template")));
      case "dynamic/custom" -> result.setDynamicCustom(DialogDynamicCustomAction.newBuilder().setId(string(json, "id")).setAdditions(json.has("additions") ? json.get("additions").toString() : ""));
      default -> { return Optional.empty(); }
    }
    return Optional.of(result.build());
  }

  private static String string(JsonObject json, String key) { return json.has(key) ? json.get(key).getAsString() : ""; }
}
