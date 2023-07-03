package net.pistonmaster.serverwrecker.protocol.bot.model;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public record ServerPlayData(Component motd, byte @Nullable [] iconBytes, boolean enforcesSecureChat) {
}
