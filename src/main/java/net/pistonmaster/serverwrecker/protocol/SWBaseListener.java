package net.pistonmaster.serverwrecker.protocol;

import com.github.steveice10.mc.protocol.ClientListener;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.handshake.HandshakeIntent;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import lombok.NonNull;
import net.pistonmaster.serverwrecker.protocol.tcp.ViaTcpClientSession;

public class SWBaseListener extends ClientListener {
    private final ProtocolState targetState;

    @Override
    public void connected(ConnectedEvent event) {
        ViaTcpClientSession session = (ViaTcpClientSession) event.getSession();
        if (this.targetState == ProtocolState.LOGIN) {
            System.out.println(session.getPacketProtocol().getServerboundId(ClientIntentionPacket.class));
            ClientIntentionPacket packet = new ClientIntentionPacket(session.getOptions().protocolVersion().getVersion(), event.getSession().getHost(), event.getSession().getPort(), HandshakeIntent.LOGIN);
            System.out.println("By packet: " + session.getPacketProtocol().getServerboundId(packet));
            try {
                session.send(packet);
            } catch (Throwable t) {
                System.out.println("Failed to send packet, trying id");
                System.out.println("Here is the id: " + session.getPacketProtocol().getServerboundId(packet));
            }

        } else if (this.targetState == ProtocolState.STATUS) {
            session.send(new ClientIntentionPacket(session.getOptions().protocolVersion().getVersion(), event.getSession().getHost(), event.getSession().getPort(), HandshakeIntent.STATUS));
        }
    }

    public SWBaseListener(@NonNull ProtocolState targetState) {
        super(targetState);
        this.targetState = targetState;
    }
}
