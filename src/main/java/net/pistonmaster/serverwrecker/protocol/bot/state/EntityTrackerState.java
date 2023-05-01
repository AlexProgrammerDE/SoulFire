package net.pistonmaster.serverwrecker.protocol.bot.state;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import net.pistonmaster.serverwrecker.protocol.bot.state.entity.EntityState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class EntityTrackerState {
    private final Map<Integer, EntityState> entities = new Int2ObjectOpenHashMap<>();
}
