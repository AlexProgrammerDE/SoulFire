package net.pistonmaster.wirebot.gui;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.packetlib.ProxyInfo;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.wirebot.WireBot;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LoadProxiesListener implements ActionListener {
    private final WireBot botManager;
    private final JFrame frame;
    private final JFileChooser fileChooser;

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            Path proxyFile = fileChooser.getSelectedFile().toPath();
            WireBot.getLogger().log(Level.INFO, "Opening: {0}.", proxyFile.getFileName());

            botManager.getThreadPool().submit(() -> {
                try {
                    List<ProxyInfo> proxies = Files.lines(proxyFile).distinct().map((line) -> {
                        String host = line.split(":")[0];
                        int port = Integer.parseInt(line.split(":")[1]);

                        InetSocketAddress address = new InetSocketAddress(host, port);
                        return new ProxyInfo(ProxyInfo.Type.SOCKS5, address);
                    }).collect(Collectors.toList());

                    WireBot.getLogger().log(Level.INFO, "Loaded {0} proxies", proxies.size());

                    botManager.setProxies(proxies);
                } catch (Exception ex) {
                    WireBot.getLogger().log(Level.SEVERE, null, ex);
                }
            });
        }
    }
}
