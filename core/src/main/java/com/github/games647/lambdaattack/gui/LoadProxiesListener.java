package com.github.games647.lambdaattack.gui;

import com.github.games647.lambdaattack.LambdaAttack;
import com.github.steveice10.packetlib.ProxyInfo;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class LoadProxiesListener implements ActionListener {

    private final LambdaAttack botManager;

    private final JFrame frame;
    private final JFileChooser fileChooser;

    public LoadProxiesListener(LambdaAttack botManager, JFrame frame, JFileChooser fileChooser) {
        this.botManager = botManager;

        this.frame = frame;
        this.fileChooser = fileChooser;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            Path proxyFile = fileChooser.getSelectedFile().toPath();
            LambdaAttack.getLogger().log(Level.INFO, "Opening: {0}.", proxyFile.getFileName());

            botManager.getThreadPool().submit(() -> {
                try {
                    List<ProxyInfo> proxies = Files.lines(proxyFile).distinct().map((line) -> {
                        String host = line.split(":")[0];
                        int port = Integer.parseInt(line.split(":")[1]);

                        InetSocketAddress address = new InetSocketAddress(host, port);
                        return new ProxyInfo(ProxyInfo.Type.SOCKS5, address);
                    }).collect(Collectors.toList());

                    LambdaAttack.getLogger().log(Level.INFO, "Loaded {0} proxies", proxies.size());

                    botManager.setProxies(proxies);
                } catch (Exception ex) {
                    LambdaAttack.getLogger().log(Level.SEVERE, null, ex);
                }
            });
        }
    }
}
