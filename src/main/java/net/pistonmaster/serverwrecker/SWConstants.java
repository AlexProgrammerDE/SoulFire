package net.pistonmaster.serverwrecker;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

import java.util.ArrayList;
import java.util.List;

public class SWConstants {
    public static final String VERSION = "1.0.0";
    public static final ProtocolVersion CURRENT_PROTOCOL_VERSION = ProtocolVersion.v1_19_4;
    public static final ProtocolVersion LATEST_SHOWN_VERSION = ProtocolVersion.v1_19_4;

    public static List<ProtocolVersion> getVersionsSorted() {
        List<ProtocolVersion> versions = new ArrayList<>();

        for (ProtocolVersion version : ProtocolVersion.getProtocols()) {
            versions.add(version);

            // We don't need to add any versions after the latest version (too experimental)
            if (version == LATEST_SHOWN_VERSION) {
                break;
            }
        }

        return versions;
    }
}
