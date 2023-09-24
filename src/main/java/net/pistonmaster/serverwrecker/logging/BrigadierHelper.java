package net.pistonmaster.serverwrecker.logging;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.pistonmaster.serverwrecker.api.ConsoleSubject;

public class BrigadierHelper {
    private BrigadierHelper() {
    }

    public static LiteralArgumentBuilder<ConsoleSubject> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static <T> RequiredArgumentBuilder<ConsoleSubject, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
}
