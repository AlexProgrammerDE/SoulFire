package net.pistonmaster.wirebot.gui;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class ShellSender extends AbstractAction {
    @Getter
    private final CommandDispatcher<ShellSender> dispatcher = new CommandDispatcher<>();
    private final Logger logger;
    @Getter
    private final List<String> commandHistory = new ArrayList<>();
    @Getter
    @Setter
    private int pointer = -1;

    {
        dispatcher.register(LiteralArgumentBuilder.<ShellSender>literal("test").executes(c -> {
            sendMessage("test");
            return 1;
        }));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        pointer = -1;

        String command = e.getActionCommand();

        if (command.isEmpty())
            return;

        ((JTextField) e.getSource()).setText(null);

        commandHistory.add(command);
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
