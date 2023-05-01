package net.pistonmaster.serverwrecker.protocol.bot.state;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import lombok.Data;
import net.kyori.adventure.text.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class PlayerListState {
    private Component header;
    private final Map<UUID, PlayerListEntry> entries = new LinkedHashMap<>();
    private Component footer;
}
