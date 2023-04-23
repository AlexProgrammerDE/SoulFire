package net.pistonmaster.serverwrecker.api.event;

import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.serverwrecker.protocol.Bot;

/**
 * This event is called when a packet is sent to the connected server.
 * Setter is used to change the packet by a plugin.
 */
@Getter
@AllArgsConstructor
public class SWPacketSendingEvent implements ServerWreckerEvent {
    private final Bot bot;
    @Setter
    private MinecraftPacket packet;
}
