package net.pistonmaster.serverwrecker.viaversion.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import net.pistonmaster.serverwrecker.protocol.SWProtocolConstants;
import net.pistonmaster.serverwrecker.protocol.tcp.ViaTcpClientSession;
import net.pistonmaster.serverwrecker.viaversion.StorableSession;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.providers.EncryptionProvider;

import javax.crypto.SecretKey;
import java.util.Objects;

public class SWViaEncryptionProvider extends EncryptionProvider {
    @Override
    public void enableDecryption(UserConnection user) {
        ViaTcpClientSession session = Objects.requireNonNull(user.get(StorableSession.class)).session();
        SecretKey key = session.getFlag(SWProtocolConstants.ENCRYPTION_SECRET_KEY);
        Objects.requireNonNull(key, "Key is null!");

        System.out.println("Enabling decryption for " + user.getProtocolInfo().getUsername() + " with key " + key);
        session.enableEncryption(key);
    }
}
