package net.pistonmaster.serverwrecker.api.event;

import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.serverwrecker.protocol.Bot;

/**
 * This event is called when a packet is received from the connected server.
 * This event is called before the packet is processed by the default bot listener.
 * Setter is used to change the packet by a plugin to change the behaviour of the bot.
 */
@Getter
@AllArgsConstructor
public class SWPacketReceiveEvent implements ServerWreckerEvent {
    private final Bot bot;
    @Setter
    private MinecraftPacket packet;
}
