/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.script.api;

import com.soulfiremc.server.protocol.BotConnection;
import org.graalvm.polyglot.HostAccess;

import java.util.List;

public record ScriptBotAPI(BotConnection connection) {
  @HostAccess.Export
  public String getId() {
    return connection.accountProfileId().toString();
  }

  @HostAccess.Export
  public String getName() {
    return connection.accountName();
  }

  @HostAccess.Export
  public void chat(String message) {
    connection.botControl().sendMessage(message);
  }

  @HostAccess.Export
  public boolean isConnected() {
    return connection.session().isConnected();
  }

  @HostAccess.Export
  public boolean isDisconnected() {
    return connection.session().isDisconnected();
  }

  @HostAccess.Export
  public void disconnect() {
    connection.session().disconnect("Disconnected by script");
  }

  @HostAccess.Export
  public ScriptMovementAPI getMovement() {
    return new ScriptMovementAPI(connection);
  }

  @HostAccess.Export
  public ScriptMetadataAPI getMetadata() {
    return new ScriptMetadataAPI(connection.metadata());
  }

  @HostAccess.Export
  public ScriptLevelAPI getLevel() {
    return new ScriptLevelAPI(connection.dataManager().currentLevel());
  }

  @HostAccess.Export
  public ScriptPlayerAPI getPlayer() {
    return new ScriptPlayerAPI(connection.dataManager().localPlayer());
  }

  @HostAccess.Export
  public List<ScriptPlayerListEntryAPI> getPlayerList() {
    return connection.dataManager().playerListState().entries().values().stream().map(ScriptPlayerListEntryAPI::new).toList();
  }
}
