package com.github.games647.lambdaattack.gui;

import com.github.games647.lambdaattack.LambdaAttack;
import java.awt.FlowLayout;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import org.spacehq.mc.auth.exception.request.RequestException;

public class MainGui {

    private static final String PROJECT_NAME = "LambdaAttack v2.0";
    
    public static void main(String[] args) {
        new MainGui();
    }

    private final JFrame frame = new JFrame(PROJECT_NAME);
    private final Logger logger = Logger.getLogger(PROJECT_NAME);
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
        JLabel amountLabel = new JLabel("Amount: ");
        JSpinner amount = new JSpinner();
        amount.setValue(20);

        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");

        startButton.addActionListener((action) -> {
            try {
                botManager.start("127.0.0.1", 25565, (int) amount.getValue(), 0);
            } catch (RequestException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        });
        stopButton.addActionListener((action) -> botManager.stop());

        panel.add(amountLabel);
        panel.add(amount);
        panel.add(startButton);
        panel.add(stopButton);

        this.frame.add(panel);
        this.frame.pack();
        this.frame.setVisible(true);
    }

    public Logger getLogger() {
        return logger;
    }
}
