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
package com.soulfiremc.server.plugins;

import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.InternalPluginClass;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.BotPacketPreReceiveEvent;
import com.soulfiremc.server.api.metadata.MetadataKey;
import com.soulfiremc.server.bot.BotConnection;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.server.dialog.Dialog;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles server-sent dialogs (Minecraft 1.21.6+).
 * This plugin intercepts dialog packets and stores the current dialog state,
 * making it available for the gRPC API to expose to clients.
 */
@Slf4j
@InternalPluginClass
public final class DialogHandler extends InternalPlugin {
  /**
   * Metadata key for storing the current dialog state per bot.
   * The value is an AtomicReference to allow thread-safe updates.
   * Contains the Holder<Dialog> from the packet.
   */
  @SuppressWarnings("unchecked")
  public static final MetadataKey<AtomicReference<Holder<Dialog>>> CURRENT_DIALOG =
    MetadataKey.of("dialog_handler", "current_dialog", (Class<AtomicReference<Holder<Dialog>>>) (Class<?>) AtomicReference.class);

  public DialogHandler() {
    super(new PluginInfo(
      "dialog-handler",
      "1.0.0",
      "Handles server-sent dialogs and exposes them via gRPC",
      "AlexProgrammerDE",
      "AGPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  /**
   * Gets the current dialog for a bot connection.
   *
   * @param connection The bot connection
   * @return The current dialog holder, or null if no dialog is displayed
   */
  @Nullable
  public static Holder<Dialog> getCurrentDialog(BotConnection connection) {
    var dialogRef = connection.metadata().get(CURRENT_DIALOG);
    return dialogRef != null ? dialogRef.get() : null;
  }

  /**
   * Sets the current dialog for a bot connection.
   *
   * @param connection The bot connection
   * @param dialog     The dialog holder to set, or null to clear
   */
  public static void setCurrentDialog(BotConnection connection, @Nullable Holder<Dialog> dialog) {
    var dialogRef = connection.metadata().getOrSet(CURRENT_DIALOG, AtomicReference::new);
    dialogRef.set(dialog);
    if (dialog != null) {
      var dialogValue = dialog.value();
      log.debug("Dialog shown: type={}", dialogValue.getClass().getSimpleName());
    } else {
      log.debug("Dialog cleared");
    }
  }

  @EventHandler
  public static void onShowDialogPacket(BotPacketPreReceiveEvent event) {
    if (!(event.packet() instanceof ClientboundShowDialogPacket showDialogPacket)) {
      return;
    }

    var dialogHolder = showDialogPacket.dialog();
    setCurrentDialog(event.connection(), dialogHolder);

    var dialog = dialogHolder.value();
    log.info("Server sent dialog: type={}", dialog.getClass().getSimpleName());
  }

  @EventHandler
  public static void onClearDialogPacket(BotPacketPreReceiveEvent event) {
    if (!(event.packet() instanceof ClientboundClearDialogPacket)) {
      return;
    }

    setCurrentDialog(event.connection(), null);
    log.info("Server cleared dialog");
  }
}
