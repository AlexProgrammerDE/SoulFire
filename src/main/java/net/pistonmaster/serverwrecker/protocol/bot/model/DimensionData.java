package net.pistonmaster.serverwrecker.protocol.bot.model;

public record DimensionData(String dimension, String worldName, long hashedSeed, boolean debug, boolean flat) {
}
