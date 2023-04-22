package net.pistonmaster.serverwrecker.viaversion;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.CompressionProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.MovementTransmitterProvider;
import com.viaversion.viaversion.velocity.providers.VelocityMovementTransmitter;
import net.pistonmaster.serverwrecker.viaversion.providers.SWViaCompressionProvider;
import net.pistonmaster.serverwrecker.viaversion.providers.SWViaEncryptionProvider;
import net.pistonmaster.serverwrecker.viaversion.providers.SWViaVersionProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.providers.EncryptionProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_8to1_7_6_10.model.GameProfile;
import net.raphimc.vialegacy.protocols.release.protocol1_8to1_7_6_10.providers.GameProfileFetcher;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class SWViaLoader implements ViaPlatformLoader {
    @Override
    public void load() {
        Via.getManager().getProviders().use(MovementTransmitterProvider.class, new VelocityMovementTransmitter());
        Via.getManager().getProviders().use(VersionProvider.class, new SWViaVersionProvider());
        Via.getManager().getProviders().use(CompressionProvider.class, new SWViaCompressionProvider());

        // For ViaLegacy
        Via.getManager().getProviders().use(GameProfileFetcher.class, new GameProfileFetcher() {
            @Override
            public UUID loadMojangUUID(String playerName) throws Exception {
                return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public GameProfile loadGameProfile(UUID uuid) throws Exception {
                return new GameProfile("Bot", uuid);
            }
        });
        Via.getManager().getProviders().use(EncryptionProvider.class, new SWViaEncryptionProvider());
    }

    @Override
    public void unload() {
    }
}
