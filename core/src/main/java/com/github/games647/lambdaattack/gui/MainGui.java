package com.github.games647.lambdaattack.gui;

import com.github.games647.lambdaattack.GameVersion;
import com.github.games647.lambdaattack.LambdaAttack;
import com.github.games647.lambdaattack.Options;
import com.github.games647.lambdaattack.logging.LogHandler;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainGui {

    private final JFrame frame = new JFrame(LambdaAttack.PROJECT_NAME);

    private final LambdaAttack botManager;

    public MainGui(LambdaAttack botManager) {
        this.botManager = botManager;

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

        topPanel.add(new JLabel("Auto Register: "));
        JCheckBox autoRegister = new JCheckBox();
        topPanel.add(autoRegister);

        topPanel.add(new JLabel("Amount: "));
        JSpinner amount = new JSpinner();
        amount.setValue(20);
        topPanel.add(amount);

        topPanel.add(new JLabel("NameFormat: "));
        JTextField nameFormat = new JTextField("Bot-%d");
        topPanel.add(nameFormat);

        JComboBox<String> versionBox = new JComboBox<>();
        Arrays.stream(GameVersion.values())
                .sorted(Comparator.reverseOrder())
                .map(GameVersion::getVersion)
                .forEach(versionBox::addItem);

        topPanel.add(versionBox);

        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        topPanel.add(startButton);
        topPanel.add(stopButton);

        JButton loadNames = new JButton("Load Names");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("", "txt"));
        loadNames.addActionListener(new LoadNamesListener(botManager, frame, fileChooser));

        topPanel.add(loadNames);

        JButton loadProxies = new JButton("Load proxies");

        loadProxies.addActionListener(new LoadProxiesListener(botManager, frame, fileChooser));

        topPanel.add(loadProxies);

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
                    LambdaAttack.getLogger().log(Level.INFO, ex.getMessage(), ex);
                }
            });
        });

        stopButton.addActionListener(action -> botManager.stop());
        return topPanel;
    }

    private JScrollPane setButtonPane() throws SecurityException {
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
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
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
