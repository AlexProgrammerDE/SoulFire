package com.github.games647.lambdaattack.version.v1_17;

import com.github.games647.lambdaattack.common.IPacketWrapper;
import com.github.steveice10.mc.protocol.MinecraftProtocol;

public class ProtocolWrapper extends MinecraftProtocol implements IPacketWrapper {
    public ProtocolWrapper(String username) {
        super(username);
    }

    @Override
    public String getProfileName() {
        return getProfile().getName();
    }
}