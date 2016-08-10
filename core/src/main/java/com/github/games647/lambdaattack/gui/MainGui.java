package com.github.games647.lambdaattack.gui;

import com.github.games647.lambdaattack.GameVersion;
import com.github.games647.lambdaattack.LambdaAttack;
import com.github.games647.lambdaattack.logging.LogHandler;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
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
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainGui {

    public static void main(String[] args) {
        new MainGui();
    }

    private final JFrame frame = new JFrame(LambdaAttack.PROJECT_NAME);
    
    private final ExecutorService threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable task) {
            Thread newThread = Executors.defaultThreadFactory().newThread(task);
            newThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    LambdaAttack.getLogger().log(Level.SEVERE, null, throwable);
                }
            });

            return newThread;
        }
    });
    
    private final LambdaAttack botManager = new LambdaAttack();

    public MainGui() {
        this.frame.setResizable(false);
        this.frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setLookAndFeel();

        JPanel topPanel = setTopPane();
        JScrollPane buttonPane = setButtonPane();

        this.frame.add(topPanel, BorderLayout.PAGE_START);
        this.frame.add(buttonPane, BorderLayout.CENTER);
        this.frame.pack();
        this.frame.setVisible(true);

        LambdaAttack.getLogger().info("Starting program");
    }

    private JPanel setTopPane() {
        //top pane
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

        JComboBox<String> versionBox = new JComboBox<>(new String[]{"1.10", "1.9", "1.8", "1.7"});
        versionBox.addItemListener((itemEvent) -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                botManager.setGameVersion(GameVersion.findByName((String) itemEvent.getItem()));
            }
        });
        topPanel.add(versionBox);

        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        topPanel.add(startButton);
        topPanel.add(stopButton);

        JButton loadProxies = new JButton("Load proxies");
        JFileChooser proxyFileChooser = new JFileChooser();
        proxyFileChooser.addChoosableFileFilter(new FileNameExtensionFilter("", "txt"));

        loadProxies.addActionListener((action) -> {
            int returnVal = proxyFileChooser.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File proxyFile = proxyFileChooser.getSelectedFile();
                LambdaAttack.getLogger().log(Level.INFO, "Opening: {0}.", proxyFile.getName());

                threadPool.submit(() -> {
                    try {
                        List<String> lines = Files.readAllLines(proxyFile.toPath());
                        List<Proxy> proxies = lines.parallelStream().map((line) -> {
                            String host = line.split(":")[0];
                            int port = Integer.parseInt(line.split(":")[1]);

                            InetSocketAddress address = new InetSocketAddress(host, port);
                            return new Proxy(Type.SOCKS, address);
                        }).collect(Collectors.toList());

                        LambdaAttack.getLogger().log(Level.INFO, "Loaded {0} proxies", proxies.size());

                        botManager.setProxies(proxies);
                    } catch (Exception ex) {
                        LambdaAttack.getLogger().log(Level.SEVERE, null, ex);
                    }
                });
            }
        });

        topPanel.add(loadProxies);

        startButton.addActionListener((action) -> {
            String host = hostInput.getText();
            int port = Integer.parseInt(portInput.getText());

            threadPool.submit(() -> {
                try {
                    botManager.start(host, port, (int) amount.getValue(), (int) delay.getValue(), nameFormat.getText());
                } catch (Exception ex) {
                    LambdaAttack.getLogger().log(Level.SEVERE, null, ex);
                }
            });
        });

        stopButton.addActionListener((action) -> botManager.stop());
        return topPanel;
    }

    private JScrollPane setButtonPane() throws SecurityException {
        //button pane
        JScrollPane buttonPane = new JScrollPane();
        buttonPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        buttonPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JTextArea logArea = new JTextArea(10, 1);
        buttonPane.getViewport().setView(logArea);

        LambdaAttack.getLogger().addHandler(new LogHandler(logArea));

        return buttonPane;
    }

    private void setLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            LambdaAttack.getLogger().log(Level.SEVERE, null, ex);
        }
    }
}
