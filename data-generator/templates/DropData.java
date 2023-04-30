package net.pistonmaster.serverwrecker.data;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public final class DropData {
    private final ItemType dropId;
    private int dropMeta = -1;
    private int minCount = -1;
    private int maxCount = -1;

    public DropData(ItemType dropId) {
        this.dropId = dropId;
    }

    public DropData(ItemType dropId, int minCount, int maxCount) {
        this.dropId = dropId;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }
}
