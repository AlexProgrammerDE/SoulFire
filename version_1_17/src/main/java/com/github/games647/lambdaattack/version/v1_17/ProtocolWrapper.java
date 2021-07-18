package com.github.games647.lambdaattack.version.v1_17;

import com.github.games647.lambdaattack.common.IPacketWrapper;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Session;

public class ProtocolWrapper extends MinecraftProtocol implements IPacketWrapper {
    public ProtocolWrapper(String username) {
        super(username);
    }

    public void newClientSession(Client var1, Session var2) {
        super.newClientSession(var2);
    }

    @Override
    public String getProfileName() {
        return getProfile().getName();
    }
}