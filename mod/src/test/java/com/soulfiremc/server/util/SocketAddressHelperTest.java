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
package com.soulfiremc.server.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.net.UnixDomainSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SocketAddressHelperTest {
  @ParameterizedTest
  @ValueSource(strings = {"inet://[::1]:1080", "inet://[2001:db8::5]:65535", "inet://127.0.0.1:8080"})
  void networkAddressesRoundTrip(String input) {
    var address = SocketAddressHelper.deserialize(input);
    var serialized = SocketAddressHelper.serialize(address);
    assertEquals(address, SocketAddressHelper.deserialize(serialized));
    assertEquals(serialized, SocketAddressHelper.serialize(SocketAddressHelper.deserialize(serialized)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"localhost", "unix:relative"})
  void incompleteUrisReportInvalidArguments(String input) {
    assertThrows(IllegalArgumentException.class, () -> SocketAddressHelper.deserialize(input));
  }

  @Test
  void unresolvedIpv6AddressesRetainTheirBrackets() {
    var address = InetSocketAddress.createUnresolved("[2001:db8::5]", 1080);
    assertEquals("inet://[2001:db8::5]:1080", SocketAddressHelper.serialize(address));
  }

  @Test
  void unixAddressesRoundTrip() {
    var address = UnixDomainSocketAddress.of("/tmp/soulfire-proxy.sock");
    assertEquals(address, SocketAddressHelper.deserialize(SocketAddressHelper.serialize(address)));
  }
}
