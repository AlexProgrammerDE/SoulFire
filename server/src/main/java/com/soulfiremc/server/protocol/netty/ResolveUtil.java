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
package com.soulfiremc.server.protocol.netty;

import com.soulfiremc.server.settings.BotSettings;
import com.soulfiremc.server.settings.lib.SettingsHolder;
import com.soulfiremc.util.ServerAddress;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Optional;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResolveUtil {
  public static final int MC_DEFAULT_PORT = 25565;
  private static final DirContext DIR_CONTEXT;

  static {
    try {
      var contextFactory = "com.sun.jndi.dns.DnsContextFactory";
      Class.forName(contextFactory);
      var environment = new Hashtable<String, String>();
      environment.put("java.naming.factory.initial", contextFactory);
      environment.put("java.naming.provider.url", "dns:");
      environment.put("com.sun.jndi.dns.timeout.retries", "1");
      DIR_CONTEXT = new InitialDirContext(environment);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private ResolveUtil() {}

  public static Optional<ResolvedAddress> resolveAddress(
    boolean isBedrock, SettingsHolder settingsHolder) {
    var serverAddress =
      ServerAddress.fromStringDefaultPort(
        settingsHolder.get(BotSettings.ADDRESS), MC_DEFAULT_PORT);

    if (settingsHolder.get(BotSettings.RESOLVE_SRV)
      && serverAddress.port() == MC_DEFAULT_PORT
      && !isBedrock) {
      // SRVs can override address on Java, but not Bedrock.
      var resolved = resolveSrv(serverAddress);
      if (resolved.isPresent()) {
        return resolved;
      }
    } else {
      log.debug("Not resolving SRV record for {}", serverAddress.host());
    }

    return resolveByHost(serverAddress).map(e -> new ResolvedAddress(serverAddress, e));
  }

  private static Optional<ResolvedAddress> resolveSrv(ServerAddress serverAddress) {
    var name = "_minecraft._tcp." + serverAddress.host();
    log.debug("Attempting SRV lookup for \"{}\".", name);

    try {
      var srvAttribute = DIR_CONTEXT.getAttributes(name, new String[] {"SRV"}).get("srv");
      if (srvAttribute != null) {
        var attributeSplit = srvAttribute.get().toString().split(" ", 4);
        log.debug("SRV lookup resolved \"{}\" to \"{}\".", name, srvAttribute.get().toString());

        return resolveByHost(
          ServerAddress.fromStringAndPort(attributeSplit[3], parsePort(attributeSplit[2])))
          .map(e -> new ResolvedAddress(serverAddress, e));
      } else {
        log.debug("SRV lookup for \"{}\" returned no records.", name);
      }
    } catch (Exception e) {
      log.debug("Failed to resolve SRV record.", e);
    }

    return Optional.empty();
  }

  private static Optional<SocketAddress> resolveByHost(ServerAddress serverAddress) {
    try {
      var host = serverAddress.host();
      var resolved = InetAddress.getByName(host);
      log.debug("Resolved {} -> {}", host, resolved.getHostAddress());
      return Optional.of(new InetSocketAddress(resolved, serverAddress.port()));
    } catch (UnknownHostException e) {
      log.debug("Failed to resolve host.", e);
      return Optional.empty();
    }
  }

  private static int parsePort(String port) {
    try {
      return Integer.parseInt(port);
    } catch (NumberFormatException e) {
      return MC_DEFAULT_PORT;
    }
  }

  public record ResolvedAddress(ServerAddress originalAddress, SocketAddress resolvedAddress) {}
}
