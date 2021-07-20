package net.pistonmaster.wirebot.version.v1_17;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Session;
import net.pistonmaster.wirebot.common.IPacketWrapper;

public class ProtocolWrapper extends MinecraftProtocol implements IPacketWrapper {
    public ProtocolWrapper(String username) {
        super(username);
    }

    public ProtocolWrapper(GameProfile profile, String accessToken) throws RequestException {
        super(profile, accessToken);
    }

    public void newClientSession(Client var1, Session var2) {
        super.newClientSession(var2);
    }

    @Override
    public String getProfileName() {
        return getProfile().getName();
    }
}