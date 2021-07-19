package net.pistonmaster.wirebot.common;

import lombok.Data;

@Data
public class Pair<L, R> {
    private final L left;
    private final R right;
}
