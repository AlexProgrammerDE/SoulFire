package com.github.games647.lambdaattack.version.v1_11;

import com.github.games647.lambdaattack.GameVersion;
import com.github.games647.lambdaattack.UniversalProtocol;

import java.net.Proxy;

import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.packetlib.packet.PacketProtocol;

public class ProtocolWrapper extends MinecraftProtocol implements UniversalProtocol {

    public ProtocolWrapper(String username) {
        super(username);
    }

    public ProtocolWrapper(String username, String password) throws RequestException {
        super(username, password);
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
        return GameVersion.VERSION_1_11;
    }
}
