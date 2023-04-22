package net.pistonmaster.serverwrecker.viaversion.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.CompressionProvider;
import net.pistonmaster.serverwrecker.viaversion.StorableSession;

import java.util.Objects;

public class SWViaCompressionProvider extends CompressionProvider {
    @Override
    public void handlePlayCompression(UserConnection user, int threshold) {
        Objects.requireNonNull(user.get(StorableSession.class)).session().setCompressionThreshold(threshold);
    }
}
