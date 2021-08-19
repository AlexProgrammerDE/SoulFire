package net.pistonmaster.serverwrecker.gui;

import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.ServerWrecker;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LoadAccountsListener implements ActionListener {
    private final ServerWrecker botManager;
    private final JFrame frame;
    private final JFileChooser fileChooser;

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            Path accountFile = fileChooser.getSelectedFile().toPath();
            ServerWrecker.getLogger().info("Opening: {}.", accountFile.getFileName());

            botManager.getThreadPool().submit(() -> {
                try {
                    List<String> accounts = Files.lines(accountFile).distinct().collect(Collectors.toList());

                    ServerWrecker.getLogger().info("Loaded {} accounts", accounts.size());
                    botManager.setAccounts(accounts);
                } catch (Exception ex) {
                    ServerWrecker.getLogger().error(null, ex);
                }
            });
        }
    }
}
