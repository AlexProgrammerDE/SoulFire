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
package com.soulfiremc.server.user;

public class Permissions {
  public static final Permission COMMAND_EXECUTION =
    new Permission("soulfire.command_execution", "Allows the client to execute commands");
  public static final Permission COMMAND_COMPLETION =
    new Permission("soulfire.command_completion", "Allows the client to tab complete commands");
  public static final Permission COMMAND_HISTORY =
    new Permission("soulfire.command_history", "Allows the client to view command history");
  public static final Permission START_ATTACK =
    new Permission("soulfire.start_attack", "Allows the client to start an attack");
  public static final Permission TOGGLE_ATTACK =
    new Permission("soulfire.toggle_attack", "Allows the client to toggle an attack");
  public static final Permission STOP_ATTACK =
    new Permission("soulfire.stop_attack", "Allows the client to stop an attack");
  public static final Permission AUTHENTICATE_MC_ACCOUNT =
    new Permission("soulfire.authenticate_mc_account", "Allows the client to authenticate or refresh a Minecraft account");
  public static final Permission CHECK_PROXY =
    new Permission("soulfire.check_proxy", "Allows the client to check if a proxy is valid");
  public static final Permission SUBSCRIBE_LOGS =
    new Permission("soulfire.subscribe_logs", "Allows the client to subscribe to logs");
  public static final Permission SERVER_CONFIG =
    new Permission("soulfire.server_config", "Allows the client to view server configuration");

  private Permissions() {
  }
}
