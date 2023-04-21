package net.pistonmaster.serverwrecker.viaversion;

import com.viaversion.viaversion.api.connection.StorableObject;
import net.pistonmaster.serverwrecker.common.SWOptions;

public record StorableOptions(SWOptions options) implements StorableObject {
    @Override
    public boolean clearOnServerSwitch() {
        return false;
    }
}
