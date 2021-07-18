package com.github.games647.lambdaattack.version.v1_7;

import com.github.games647.lambdaattack.GameVersion;
import com.github.games647.lambdaattack.UniversalProtocol;

import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.auth.exception.AuthenticationException;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.packetlib.packet.PacketProtocol;

public class ProtocolWrapper extends MinecraftProtocol implements UniversalProtocol {

    public ProtocolWrapper(String username) {
        super(username);
    }

    public ProtocolWrapper(String username, String using, boolean token) throws AuthenticationException {
        super(username, using, token);
    }

    public ProtocolWrapper(GameProfile profile, String accessToken) {
        super(new org.spacehq.mc.auth.GameProfile(profile.getId(), profile.getName()), accessToken);
    }

    @Override
    public PacketProtocol getProtocol() {
        return this;
    }

    @Override
    public GameProfile getGameProfile() {
        org.spacehq.mc.auth.GameProfile profile = super.getProfile();
        return new GameProfile(profile.getId(), profile.getName());
    }

    @Override
    public GameVersion getGameVersion() {
        return GameVersion.VERSION_1_7;
    }
}
