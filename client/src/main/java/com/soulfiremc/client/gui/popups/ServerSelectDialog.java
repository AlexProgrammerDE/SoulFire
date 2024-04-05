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

import com.soulfiremc.client.RemoteServerData;
import com.soulfiremc.client.gui.libs.SFSwingUtils;
import com.soulfiremc.util.PortHelper;
import com.soulfiremc.util.ServerAddress;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
  public ServerSelectDialog(
    Runnable integratedServerRunnable, Consumer<RemoteServerData> remoteServerConsumer) {
    setTitle("Connect to a SoulFire Server");

    SFSwingUtils.setLogo(this);

    var mainPanel = new JPanel(new GridBagLayout());
    var gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(5, 5, 5, 5);

    // Left panel for integrated server button
    var leftPanel = new JPanel();
    leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
    leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    var forBeginnersLabel = new JLabel("Run SoulFire locally:");
    leftPanel.add(forBeginnersLabel);

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

    var addressLabel = new JLabel("Address:");
    var addressField = new JTextField(10);
    var tokenLabel = new JLabel("Token:");
    var tokenField = new JPasswordField(10);
    var submitButton = new JButton("Connect");
    submitButton.addActionListener(
      e -> {
        // Check if all fields are filled
        if (addressField.getText().isEmpty() || tokenField.getPassword().length == 0) {
          JOptionPane.showMessageDialog(
            this, "Please fill in all fields.", "Invalid input", JOptionPane.WARNING_MESSAGE);
        } else {
          // Add action to handle using remote server
          var address = addressField.getText();
          var token = new String(tokenField.getPassword());

          Executors.newSingleThreadExecutor()
            .execute(
              () ->
                remoteServerConsumer.accept(
                  new RemoteServerData(
                    ServerAddress.fromStringDefaultPort(
                      address, PortHelper.SF_DEFAULT_PORT),
                    token)));
          setVisible(false);
          dispose();
        }
      });

    rightPanel.add(addressLabel, gbcRight);
    gbcRight.gridy++;
    rightPanel.add(addressField, gbcRight);
    gbcRight.gridy++;
    rightPanel.add(tokenLabel, gbcRight);
    gbcRight.gridy++;
    rightPanel.add(tokenField, gbcRight);
    gbcRight.gridy++;
    gbcRight.gridwidth = 2;
    gbcRight.fill = GridBagConstraints.HORIZONTAL;
    rightPanel.add(submitButton, gbcRight);
    mainPanel.add(rightPanel, gbc);

    add(mainPanel);

    pack();

    setMinimumSize(new Dimension(getWidth(), getHeight()));

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    setVisible(true);
  }
}
