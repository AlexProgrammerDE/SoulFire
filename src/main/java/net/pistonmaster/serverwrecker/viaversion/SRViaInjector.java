package net.pistonmaster.serverwrecker.viaversion;

import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.fastutil.ints.IntLinkedOpenHashSet;
import com.viaversion.viaversion.libs.fastutil.ints.IntSortedSet;
import com.viaversion.viaversion.libs.gson.JsonObject;
import net.pistonmaster.serverwrecker.SWConstants;

public class SRViaInjector implements ViaInjector {
    @Override
    public void inject() {
    }

    @Override
    public void uninject() {
    }

    @Override
    public IntSortedSet getServerProtocolVersions() {
        // On client-side we can connect to any server version
        IntSortedSet versions = new IntLinkedOpenHashSet();
        versions.add(ProtocolVersion.v1_8.getOriginalVersion());
        versions.add(SWConstants.getVersionsSorted()
                .stream()
                .mapToInt(ProtocolVersion::getOriginalVersion)
                .max().getAsInt());
        return versions;
    }

    @Override
    public int getServerProtocolVersion() {
        return getServerProtocolVersions().firstInt();
    }

    @Override
    public String getEncoderName() {
        return getDecoderName();
    }

    @Override
    public String getDecoderName() {
        return "via-codec";
    }

    @Override
    public JsonObject getDump() {
        return new JsonObject();
    }
}
