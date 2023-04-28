/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.gui;

import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.common.BotProxy;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class LoadProxiesListener implements ActionListener {
    private final ServerWrecker serverWrecker;
    private final JFrame frame;
    private final JFileChooser fileChooser;

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path proxyFile = fileChooser.getSelectedFile().toPath();
        ServerWrecker.getLogger().info("Opening: {}.", proxyFile.getFileName());

        serverWrecker.getThreadPool().submit(() -> {
            try {
                List<BotProxy> proxies = new ArrayList<>();

                try (Stream<String> lines = Files.lines(proxyFile)) {
                    lines.distinct().forEach(line -> {
                        String[] split = line.split(":");

                        String host = split[0];
                        int port = Integer.parseInt(split[1]);

                        if (split.length > 3) {
                            proxies.add(new BotProxy(new InetSocketAddress(host, port), split[2], split[3]));
                        } else {
                            proxies.add(new BotProxy(new InetSocketAddress(host, port), null, null));
                        }
                    });
                }

                serverWrecker.getPassWordProxies().clear();
                serverWrecker.getPassWordProxies().addAll(proxies);

                ServerWrecker.getLogger().info("Loaded {} proxies", proxies.size());
            } catch (Exception ex) {
                ServerWrecker.getLogger().error(null, ex);
            }
        });
    }
}
