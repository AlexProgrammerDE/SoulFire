package com.github.games647.lambdaattack.gui;

import com.github.games647.lambdaattack.LambdaAttack;
import com.github.games647.lambdaattack.logging.LogHandler;
import java.awt.BorderLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;


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

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Host: "));
        JTextField hostInput = new JTextField("127.0.0.1");
        topPanel.add(hostInput);
        topPanel.add(new JLabel("Port: "));
        JTextField portInput = new JTextField("25565");
        topPanel.add(portInput);


        topPanel.add(new JLabel("Join delay (ms): "));
        JSpinner delay = new JSpinner();
        delay.setValue(1000);
        topPanel.add(delay);

        topPanel.add(new JLabel("Amount: "));
        JSpinner amount = new JSpinner();
        amount.setValue(20);
        topPanel.add(amount);

        topPanel.add(new JLabel("NameFormat: "));
        JTextField nameFormat = new JTextField("Bot-%d");
        topPanel.add(nameFormat);

        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        topPanel.add(startButton);
        topPanel.add(stopButton);

        JTextArea logArea = new JTextArea(10, 1);

        JScrollPane buttonPane = new JScrollPane();
        buttonPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        buttonPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        buttonPane.getViewport().setView(logArea);
        
        Logger logger = LambdaAttack.getLogger();

        logger.addHandler(new LogHandler(logArea));

        logger.info("Starting program " + LambdaAttack.PROJECT_NAME);

        startButton.addActionListener((action) -> {
            String host = hostInput.getText();
            int port = Integer.parseInt(portInput.getText());

            threadPool.submit(() -> {
                try {
                    botManager.start(host, port, (int) amount.getValue(), (int) delay.getValue(), nameFormat.getText());
                } catch (Exception ex) {
                    botManager.getLogger().log(Level.SEVERE, null, ex);
                }
            });
        });
        
        stopButton.addActionListener((action) -> botManager.stop());

        this.frame.add(topPanel, BorderLayout.PAGE_START);
        this.frame.add(buttonPane, BorderLayout.CENTER);
        this.frame.pack();
        this.frame.setVisible(true);
    }
}
