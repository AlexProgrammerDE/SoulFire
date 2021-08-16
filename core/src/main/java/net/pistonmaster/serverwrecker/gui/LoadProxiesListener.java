package net.pistonmaster.serverwrecker.gui;

import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.ServerWrecker;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@RequiredArgsConstructor
public class LoadProxiesListener implements ActionListener {
    private final ServerWrecker botManager;
    private final JFrame frame;
    private final JFileChooser fileChooser;

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            Path proxyFile = fileChooser.getSelectedFile().toPath();
            ServerWrecker.getLogger().log(Level.INFO, "Opening: {0}.", proxyFile.getFileName());

            botManager.getThreadPool().submit(() -> {
                try {
                    Map<InetSocketAddress, PasswordAuthentication> proxies = new HashMap<>();

                    Files.lines(proxyFile).distinct().forEach((line) -> {
                        String[] split = line.split(":");

                        String host = split[0];
                        int port = Integer.parseInt(split[1]);

                        if (split.length > 3) {
                            proxies.put(new InetSocketAddress(host, port), new PasswordAuthentication(split[2], split[3].toCharArray()));
                        } else {
                            proxies.put(new InetSocketAddress(host, port), null);
                        }
                    });

                    ServerWrecker.getLogger().log(Level.INFO, "Loaded {0} proxies", proxies.size());

                    botManager.getPassWordProxies().clear();
                    botManager.getPassWordProxies().putAll(proxies);
                } catch (Exception ex) {
                    ServerWrecker.getLogger().log(Level.SEVERE, null, ex);
                }
            });
        }
    }
}
