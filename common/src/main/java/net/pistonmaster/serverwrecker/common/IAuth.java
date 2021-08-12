package net.pistonmaster.serverwrecker.common;

import java.net.Proxy;

public interface IAuth {
    IPacketWrapper authenticate(GameVersion gameVersion, String username, String password, Proxy proxy, ServiceServer serviceServer) throws Exception;
}
