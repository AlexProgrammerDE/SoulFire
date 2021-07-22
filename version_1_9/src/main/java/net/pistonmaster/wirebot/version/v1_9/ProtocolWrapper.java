package net.pistonmaster.wirebot.version.v1_9;

import net.pistonmaster.wirebot.common.IPacketWrapper;
import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.protocol.MinecraftProtocol;

import java.net.Proxy;

public class ProtocolWrapper extends MinecraftProtocol implements IPacketWrapper {
    public ProtocolWrapper(String username) {
        super(username);
    }

    public ProtocolWrapper(GameProfile profile, String accessToken) throws RequestException {
        super(profile, accessToken);
    }

    public ProtocolWrapper(String username, String using, boolean token) throws RequestException {
        super(username, using, token);
    }

    public ProtocolWrapper(String username, String using, boolean token, Proxy authProxy) throws RequestException {
        super(username, using, token, authProxy);
    }

    @Override
    public String getProfileName() {
        return getProfile().getName();
    }
}
