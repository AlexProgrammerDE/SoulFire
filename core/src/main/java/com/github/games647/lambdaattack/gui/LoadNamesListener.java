package com.github.games647.lambdaattack.gui;

import com.github.games647.lambdaattack.LambdaAttack;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class LoadNamesListener implements ActionListener {

    private final LambdaAttack botManager;

    private final JFrame frame;
    private final JFileChooser fileChooser;

    public LoadNamesListener(LambdaAttack botManager, JFrame frame, JFileChooser fileChooser) {
        this.botManager = botManager;

        this.frame = frame;
        this.fileChooser = fileChooser;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File proxyFile = fileChooser.getSelectedFile();
            LambdaAttack.getLogger().log(Level.INFO, "Opening: {0}.", proxyFile.getName());

            botManager.getThreadPool().submit(() -> {
                try {
                    List<String> lines = Files.readAllLines(proxyFile.toPath());
                    List<String> names = lines.stream().distinct().collect(Collectors.toList());

                    LambdaAttack.getLogger().log(Level.INFO, "Loaded {0} names", names.size());
                    botManager.setNames(names);
                } catch (Exception ex) {
                    LambdaAttack.getLogger().log(Level.SEVERE, null, ex);
                }
            });
        }
    }
}
