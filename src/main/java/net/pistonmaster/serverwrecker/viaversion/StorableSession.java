package net.pistonmaster.serverwrecker.viaversion;

import com.viaversion.viaversion.api.connection.StorableObject;
import net.pistonmaster.serverwrecker.protocol.tcp.ViaTcpClientSession;

public record StorableSession(ViaTcpClientSession session) implements StorableObject {
    @Override
    public boolean clearOnServerSwitch() {
        return false;
    }
}
