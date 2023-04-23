package net.pistonmaster.serverwrecker.protocol.bot.model;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import lombok.NonNull;

public record LoginPacketData(int entityId, boolean hardcore, @NonNull String[] worldNames,
                              @NonNull CompoundTag registry, int maxPlayers,
                              boolean reducedDebugInfo) {
}
