package net.pistonmaster.wirebot.version.v1_14;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import net.pistonmaster.wirebot.common.IPacketWrapper;

public class ProtocolWrapper extends MinecraftProtocol implements IPacketWrapper {
    public ProtocolWrapper(String username) {
        super(username);
    }

    public ProtocolWrapper(GameProfile profile, String clientToken, String accessToken) {
        super(profile, clientToken, accessToken);
    }

    @Override
    public String getProfileName() {
        return getProfile().getName();
    }
}
