package net.pistonmaster.serverwrecker.viaversion.providers;

import net.raphimc.vialegacy.protocols.release.protocol1_8to1_7_6_10.model.GameProfile;
import net.raphimc.vialegacy.protocols.release.protocol1_8to1_7_6_10.providers.GameProfileFetcher;

import java.util.UUID;

public class SWViaGameProfileFetcher extends GameProfileFetcher {
    @Override
    public UUID loadMojangUUID(String playerName) {
        throw new UnsupportedOperationException("This is not supported!");
    }

    @Override
    public GameProfile loadGameProfile(UUID uuid) {
        throw new UnsupportedOperationException("This is not supported!");
    }
}
