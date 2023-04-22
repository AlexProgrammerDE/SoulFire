package net.pistonmaster.serverwrecker.viaversion.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import net.pistonmaster.serverwrecker.viaversion.StorableOptions;

import java.util.Objects;

public class SWViaVersionProvider implements VersionProvider {

    @Override
    public int getClosestServerProtocol(UserConnection connection) {
        StorableOptions options = connection.get(StorableOptions.class);
        Objects.requireNonNull(options, "StorableOptions is null");

        return options.options().protocolVersion().getVersion();
    }
}
