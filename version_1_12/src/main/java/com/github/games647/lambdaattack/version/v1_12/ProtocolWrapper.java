package com.github.games647.lambdaattack.version.v1_12;

import com.github.games647.lambdaattack.GameVersion;
import com.github.games647.lambdaattack.UniversalProtocol;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.packet.PacketProtocol;

import java.net.Proxy;

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
    public GameVersion getGameVersion() {
        return GameVersion.VERSION_1_12;
    }
}
