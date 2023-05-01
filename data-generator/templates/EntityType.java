package net.pistonmaster.serverwrecker.data;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public record EntityType(int id, int internalId, String name, String displayName, String type,
        double width, double height, int length, int offset, String category) {
    public static final List<EntityType> VALUES = new ArrayList<>();

    // VALUES REPLACE

    private final int id;
    private final int internalId;
    private final String name;
    private final String displayName;
    private final String type;
    private final double width;
    private final double height;
    private final int length;
    private final int offset;
    private final String category;

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
