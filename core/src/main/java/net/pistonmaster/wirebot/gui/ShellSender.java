package net.pistonmaster.wirebot.gui;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

@RequiredArgsConstructor
public class ShellSender extends AbstractAction {
    private final CommandDispatcher<ShellSender> dispatcher = new CommandDispatcher<>();
    private final Logger logger;

    {
        dispatcher.register(LiteralArgumentBuilder.<ShellSender>literal("test").executes(c -> {
            sendMessage("test");
            return 1;
        }));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (command.isEmpty())
            return;

        ((JTextField)e.getSource()).setText(null);

        try {
            dispatcher.execute(command, this);
        } catch (CommandSyntaxException commandSyntaxException) {
            logger.warning("Invalid command syntax");
        }
    }

    public void sendMessage(String message) {
        logger.info(message);
    }
}
