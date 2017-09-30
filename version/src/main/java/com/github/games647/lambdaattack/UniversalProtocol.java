package com.github.games647.lambdaattack;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.packetlib.packet.PacketProtocol;

public interface UniversalProtocol {

    GameProfile getProfile();

    PacketProtocol getProtocol();

    GameVersion getGameVersion();
}
