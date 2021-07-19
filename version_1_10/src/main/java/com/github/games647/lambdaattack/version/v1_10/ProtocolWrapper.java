package net.pistonmaster.wirebot.version.v1_10;

import net.pistonmaster.wirebot.GameVersion;
import net.pistonmaster.wirebot.UniversalProtocol;

import java.net.Proxy;

import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.packetlib.packet.PacketProtocol;

public class ProtocolWrapper extends MinecraftProtocol implements UniversalProtocol {

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

    public ProtocolWrapper(GameProfile profile, String accessToken) {
        super(profile, accessToken);
    }

    @Override
    public PacketProtocol getProtocol() {
        return this;
    }

    @Override
    public GameProfile getGameProfile() {
        return super.getProfile();
    }

    @Override
    public GameVersion getGameVersion() {
        return GameVersion.VERSION_1_10;
    }
}
