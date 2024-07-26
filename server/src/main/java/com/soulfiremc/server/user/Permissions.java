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
  public static final Permission CREATE_INSTANCES =
    new Permission("soulfire.create_instances", "Allows the client to create an instance");
  public static final Permission UPDATE_INSTANCES =
    new Permission("soulfire.update_instances", "Allows the client to update an instance");
  public static final Permission DELETE_INSTANCES =
    new Permission("soulfire.delete_instances", "Allows the client to delete an instance");
  public static final Permission CHANGE_INSTANCE_STATE =
    new Permission("soulfire.change_instance_state", "Allows the client to change the state of an instance");
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
