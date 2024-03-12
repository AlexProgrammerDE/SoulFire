/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.client.gui.popups;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class ServerSelectDialog extends JFrame {
  private final JTextField hostField;
  private final JTextField portField;
  private final JPasswordField tokenField;

  public ServerSelectDialog(
      Runnable integratedServerRunnable, Consumer<RemoteServerData> remoteServerConsumer) {
    setTitle("Connect to SoulFire Server");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(400, 300);
    setLocationRelativeTo(null);

    var mainPanel = new JPanel(new GridBagLayout());
    var gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(5, 5, 5, 5);

    // Left panel for integrated server button
    var leftPanel = new JPanel();
    leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
    leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    var integratedServerButton = new JButton("Use Integrated Server");
    integratedServerButton.addActionListener(
        e -> {
          Executors.newSingleThreadExecutor().execute(integratedServerRunnable);
          setVisible(false);
          dispose();
        });
    leftPanel.add(integratedServerButton);
    mainPanel.add(leftPanel, gbc);

    // Add separator
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.VERTICAL;
    var separator = new JSeparator(SwingConstants.VERTICAL);
    mainPanel.add(separator, gbc);

    // Right panel for remote server settings
    gbc.gridx = 2;
    var rightPanel = new JPanel(new GridBagLayout());
    rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    var gbcRight = new GridBagConstraints();
    gbcRight.gridx = 0;
    gbcRight.gridy = 0;
    gbcRight.anchor = GridBagConstraints.WEST;
    gbcRight.insets = new Insets(2, 2, 2, 2);

    var hostLabel = new JLabel("Host:");
    hostField = new JTextField(10);
    var portLabel = new JLabel("Port:");
    portField = new JTextField(10);
    portField.addKeyListener(
        new KeyAdapter() {
          public void keyTyped(KeyEvent e) {
            var c = e.getKeyChar();
            if (!(Character.isDigit(c) || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE)) {
              e.consume();
            }
          }
        });
    var tokenLabel = new JLabel("Token:");
    tokenField = new JPasswordField(10);
    var submitButton = new JButton("Submit");
    submitButton.addActionListener(
        e -> {
          // Check if all fields are filled
          if (hostField.getText().isEmpty()
              || portField.getText().isEmpty()
              || tokenField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(ServerSelectDialog.this, "Please fill in all fields.");
          } else {
            // Add action to handle using remote server
            var host = hostField.getText();
            var port = portField.getText();
            var token = new String(tokenField.getPassword());

            Executors.newSingleThreadExecutor().execute(
                () ->
                    remoteServerConsumer.accept(
                        new RemoteServerData(host, Integer.parseInt(port), token)));
            setVisible(false);
            dispose();
          }
        });

    rightPanel.add(hostLabel, gbcRight);
    gbcRight.gridy++;
    rightPanel.add(hostField, gbcRight);
    gbcRight.gridy++;
    rightPanel.add(portLabel, gbcRight);
    gbcRight.gridy++;
    rightPanel.add(portField, gbcRight);
    gbcRight.gridy++;
    rightPanel.add(tokenLabel, gbcRight);
    gbcRight.gridy++;
    rightPanel.add(tokenField, gbcRight);
    gbcRight.gridy++;
    gbcRight.gridwidth = 2;
    rightPanel.add(submitButton, gbcRight);
    mainPanel.add(rightPanel, gbc);

    add(mainPanel);

    pack();

    setVisible(true);
  }

  public record RemoteServerData(String host, int port, String token) {}
}
