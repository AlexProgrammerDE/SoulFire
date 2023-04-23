package net.pistonmaster.serverwrecker.protocol.bot.model;

import com.nukkitx.math.vector.Vector3i;

/**
 * Represents the default spawn data. (Where the compass points to)
 * @param position The position of the spawn
 * @param angle The angle of the spawn position
 */
public record DefaultSpawnData(Vector3i position, float angle) {
}
