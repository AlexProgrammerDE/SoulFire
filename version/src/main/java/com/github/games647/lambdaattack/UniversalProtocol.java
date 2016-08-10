package com.github.games647.lambdaattack;

import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.packetlib.packet.PacketProtocol;

public interface UniversalProtocol {

    GameProfile getGameProfile();

    PacketProtocol getProtocol();

    GameVersion getGameVersion();
}
