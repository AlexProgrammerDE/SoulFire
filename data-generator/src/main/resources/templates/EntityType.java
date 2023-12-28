package net.pistonmaster.serverwrecker.data;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public record EntityType(int id, String name, float width, float height,
                         String category, boolean friendly) {
    public static final List<EntityType> VALUES = new ArrayList<>();

    // VALUES REPLACE

    public static EntityType register(String name) {
        var entityType = GsonDataHelper.fromJson("/minecraft/entities.json", name, EntityType.class);
        VALUES.add(entityType);
        return entityType;
    }

    public static EntityType getById(int id) {
        for (var entityId : VALUES) {
            if (entityId.id() == id) {
                return entityId;
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityType entityType)) return false;
        return id == entityType.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
