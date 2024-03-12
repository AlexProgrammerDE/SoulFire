package com.soulfiremc.util;

import com.google.common.net.HostAndPort;
import java.net.IDN;

public record ServerAddress(HostAndPort hostAndPort) {
  public static ServerAddress fromStringDefaultPort(String address, int defaultPort) {
    return new ServerAddress(HostAndPort.fromString(address).withDefaultPort(25565));
  }

  public static ServerAddress fromStringAndPort(String host, int port) {
    return new ServerAddress(HostAndPort.fromParts(host, port));
  }

  public String host() {
    try {
      return IDN.toASCII(hostAndPort.getHost());
    } catch (IllegalArgumentException e) {
      return "";
    }
  }

  public int port() {
    return hostAndPort.getPort();
  }
}
