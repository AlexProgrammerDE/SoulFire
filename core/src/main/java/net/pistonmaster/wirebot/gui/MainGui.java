package net.pistonmaster.wirebot.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import net.pistonmaster.wirebot.WireBot;
import net.pistonmaster.wirebot.common.GameVersion;
import net.pistonmaster.wirebot.common.Options;
import net.pistonmaster.wirebot.logging.LogHandler;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;

public class MainGui {
    private final JFrame frame;
    private final WireBot botManager;

    public MainGui(WireBot botManager) {
        this.botManager = botManager;

        setLookAndFeel();

        frame = new JFrame(WireBot.PROJECT_NAME);
        this.frame.setResizable(true);
        this.frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel leftPanel = setLogPane();
        JPanel rightPanel = setTopPane();

        frame.setLayout(new BorderLayout());
        this.frame.add(leftPanel, BorderLayout.CENTER);
        this.frame.add(rightPanel, BorderLayout.EAST);

        this.frame.pack();

        this.frame.setVisible(true);

        WireBot.getLogger().info("Started program");
    }

    private JPanel setTopPane() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new GridLayout(0, 2));

        rightPanel.add(new JLabel("Host: "));
        JTextField hostInput = new JTextField("127.0.0.1");
        rightPanel.add(hostInput);

        rightPanel.add(new JLabel("Port: "));
        JTextField portInput = new JTextField("25565");
        rightPanel.add(portInput);

        rightPanel.add(new JLabel("Join delay (ms): "));
        JSpinner delay = new JSpinner();
        delay.setValue(1000);
        rightPanel.add(delay);

        rightPanel.add(new JLabel("Auto Register: "));
        JCheckBox autoRegister = new JCheckBox();
        rightPanel.add(autoRegister);

        rightPanel.add(new JLabel("Amount: "));
        JSpinner amount = new JSpinner();
        amount.setValue(20);
        rightPanel.add(amount);

        rightPanel.add(new JLabel("NameFormat: "));
        JTextField nameFormat = new JTextField("Bot-%d");
        rightPanel.add(nameFormat);

        JComboBox<String> versionBox = new JComboBox<>();
        Arrays.stream(GameVersion.values())
                .sorted(Comparator.reverseOrder())
                .map(GameVersion::getVersion)
                .forEach(versionBox::addItem);

        rightPanel.add(versionBox);

        JPanel startStopPanel = new JPanel();
        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        startStopPanel.add(startButton);
        startStopPanel.add(stopButton);
        rightPanel.add(startStopPanel);

        JPanel loadPanel = new JPanel();
        JButton loadNames = new JButton("Load Names");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("", "txt"));
        loadNames.addActionListener(new LoadAccountsListener(botManager, frame, fileChooser));

        loadPanel.add(loadNames);

        JButton loadProxies = new JButton("Load proxies");

        loadProxies.addActionListener(new LoadProxiesListener(botManager, frame, fileChooser));

        loadPanel.add(loadProxies);

        rightPanel.add(loadPanel);

        startButton.addActionListener((action) -> {
            // collect the options on the gui thread
            // for thread-safety
            Options options = new Options(
                    hostInput.getText(),
                    Integer.parseInt(portInput.getText()),
                    (int) amount.getValue(),
                    (int) delay.getValue(),
                    nameFormat.getText(),
                    GameVersion.findByName((String) versionBox.getSelectedItem()),
                    autoRegister.isSelected());

            botManager.getThreadPool().submit(() -> {
                try {
                    botManager.start(options);
                } catch (Exception ex) {
                    WireBot.getLogger().log(Level.INFO, ex.getMessage(), ex);
                }
            });
        });

        stopButton.addActionListener(action -> botManager.stop());

        return rightPanel;
    }

    private JPanel setLogPane() throws SecurityException {
        JPanel leftPanel = new JPanel();

        JScrollPane logPane = new JScrollPane();
        logPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        logPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JTextArea logArea = new JTextArea(10, 1);
        logPane.setViewportView(logArea);

        WireBot.getLogger().addHandler(new LogHandler(logArea));

        JTextField commands = new JTextField();
        new GhostText(commands, "Type WireBot commands here...");

        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(logPane, BorderLayout.CENTER);
        leftPanel.add(commands, BorderLayout.SOUTH);

        return leftPanel;
    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
    }
}
