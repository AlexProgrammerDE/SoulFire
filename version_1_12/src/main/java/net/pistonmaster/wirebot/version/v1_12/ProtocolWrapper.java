package net.pistonmaster.wirebot.version.v1_12;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import net.pistonmaster.wirebot.common.IPacketWrapper;

public class ProtocolWrapper extends MinecraftProtocol implements IPacketWrapper {
    public ProtocolWrapper(String username) {
        super(username);
    }

    @Override
    public String getProfileName() {
        return getProfile().getName();
    }
}
