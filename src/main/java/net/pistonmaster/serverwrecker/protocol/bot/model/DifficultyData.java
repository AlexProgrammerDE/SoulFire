package net.pistonmaster.serverwrecker.protocol.bot.model;

import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;

public record DifficultyData(Difficulty difficulty, boolean locked) {
}
