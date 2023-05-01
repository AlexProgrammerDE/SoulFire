package net.pistonmaster.serverwrecker.data;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public record EntityType(int id, int internalId, String name, String displayName, String type,
        double width, double height, int length, int offset, String category) {
    public static final List<EntityType> VALUES = new ArrayList<>();

    // VALUES REPLACE

    public static EntityType register(EntityType entityType) {
        VALUES.add(entityType);
        return entityType;
    }

    public static EntityType getById(int id) {
        for (EntityType entityId : VALUES) {
            if (entityId.id() == id) {
                return entityId;
            }
        }

        return null;
    }
}
