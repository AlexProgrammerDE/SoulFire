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

import java.util.ArrayList;
import java.util.List;

public class Permissions {
  public static final List<Permission> VALUES = new ArrayList<>();
  public static final Permission.Instance COMMAND_EXECUTION =
    register(Permission.instance("soulfire.command_execution", "Allows the client to execute commands"));
  public static final Permission.Instance COMMAND_COMPLETION =
    register(Permission.instance("soulfire.command_completion", "Allows the client to tab complete commands"));
  public static final Permission.Global CREATE_INSTANCE =
    register(Permission.global("soulfire.create_instance", "Allows the client to create an instance"));
  public static final Permission.Instance READ_INSTANCE =
    register(Permission.instance("soulfire.read_instance", "Allows the client to see an instance"));
  public static final Permission.Instance UPDATE_INSTANCE =
    register(Permission.instance("soulfire.update_instance", "Allows the client to update an instance"));
  public static final Permission.Instance DELETE_INSTANCE =
    register(Permission.instance("soulfire.delete_instance", "Allows the client to delete an instance"));
  public static final Permission.Instance CHANGE_INSTANCE_STATE =
    register(Permission.instance("soulfire.change_instance_state", "Allows the client to change the state of an instance"));
  public static final Permission.Instance AUTHENTICATE_MC_ACCOUNT =
    register(Permission.instance("soulfire.authenticate_mc_account", "Allows the client to authenticate or refresh a Minecraft account"));
  public static final Permission.Instance CHECK_PROXY =
    register(Permission.instance("soulfire.check_proxy", "Allows the client to check if a proxy is valid"));
  public static final Permission.Global SUBSCRIBE_LOGS =
    register(Permission.global("soulfire.subscribe_logs", "Allows the client to subscribe to logs"));
  public static final Permission.Global SERVER_CONFIG =
    register(Permission.global("soulfire.server_config", "Allows the client to view server configuration"));
  public static final Permission.Instance DOWNLOAD_URL =
    register(Permission.instance("soulfire.download_url", "Allows the client to download data through the server from arbitrary URLs"));

  public static <T extends Permission> T register(T permission) {
    VALUES.add(permission);
    return permission;
  }

  private Permissions() {
  }
}
