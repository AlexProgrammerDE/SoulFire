package net.pistonmaster.serverwrecker.viaversion;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.CompressionProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.MovementTransmitterProvider;
import com.viaversion.viaversion.velocity.providers.VelocityMovementTransmitter;
import net.pistonmaster.serverwrecker.viaversion.providers.SWViaCompressionProvider;
import net.pistonmaster.serverwrecker.viaversion.providers.SWViaVersionProvider;

public class SWViaLoader implements ViaPlatformLoader {
    @Override
    public void load() {
        Via.getManager().getProviders().use(MovementTransmitterProvider.class, new VelocityMovementTransmitter());
        Via.getManager().getProviders().use(VersionProvider.class, new SWViaVersionProvider());
        Via.getManager().getProviders().use(CompressionProvider.class, new SWViaCompressionProvider());
    }

    @Override
    public void unload() {
    }
}
