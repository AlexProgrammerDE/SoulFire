package net.pistonmaster.wirebot.gui;

import lombok.RequiredArgsConstructor;
import net.pistonmaster.wirebot.WireBot;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LoadAccountsListener implements ActionListener {
    private final WireBot botManager;
    private final JFrame frame;
    private final JFileChooser fileChooser;

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            Path accountFile = fileChooser.getSelectedFile().toPath();
            WireBot.getLogger().log(Level.INFO, "Opening: {0}.", accountFile.getFileName());

            botManager.getThreadPool().submit(() -> {
                try {
                    List<String> accounts = Files.lines(accountFile).distinct().collect(Collectors.toList());

                    WireBot.getLogger().log(Level.INFO, "Loaded {0} accounts", accounts.size());
                    botManager.setAccounts(accounts);
                } catch (Exception ex) {
                    WireBot.getLogger().log(Level.SEVERE, null, ex);
                }
            });
        }
    }
}
