package com.github.games647.lambdaattack.gui;

import com.github.games647.lambdaattack.LambdaAttack;

import java.awt.FlowLayout;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.spacehq.mc.auth.exception.request.RequestException;

public class MainGui {
    
    public static void main(String[] args) {
        new MainGui();
    }

    private final JFrame frame = new JFrame(LambdaAttack.PROJECT_NAME);
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final LambdaAttack botManager = new LambdaAttack();

    public MainGui() {
        this.frame.setResizable(false);
        this.frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | UnsupportedLookAndFeelException ex) {
           Logger.getLogger(MainGui.class.getName()).log(Level.SEVERE, null, ex);
        }

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new JLabel("Host: "));
        JTextField hostInput = new JTextField("127.0.0.1");
        panel.add(hostInput);
        panel.add(new JLabel("Port: "));
        JTextField portInput = new JTextField("25565");
        panel.add(portInput);


        panel.add(new JLabel("Join delay (ms): "));
        JSpinner delay = new JSpinner();
        delay.setValue(1000);
        panel.add(delay);

        panel.add(new JLabel("Amount: "));
        JSpinner amount = new JSpinner();
        amount.setValue(20);
        panel.add(amount);

        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");

        startButton.addActionListener((action) -> {
            String host = hostInput.getText();
            int port = Integer.parseInt(portInput.getText());

            threadPool.submit(() -> {
                try {
                    botManager.start(host, port, (int) amount.getValue(), (int) delay.getValue());
                } catch (RequestException ex) {
                    botManager.getLogger().log(Level.SEVERE, null, ex);
                }
            });
        });
        
        stopButton.addActionListener((action) -> botManager.stop());

        panel.add(amount);
        panel.add(startButton);
        panel.add(stopButton);

        this.frame.add(panel);
        this.frame.pack();
        this.frame.setVisible(true);
    }
}
