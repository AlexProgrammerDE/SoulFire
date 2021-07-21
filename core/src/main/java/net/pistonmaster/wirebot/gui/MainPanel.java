package net.pistonmaster.wirebot.gui;

import net.pistonmaster.wirebot.WireBot;
import net.pistonmaster.wirebot.gui.navigation.RightPanelContainer;
import net.pistonmaster.wirebot.logging.LogHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MainPanel extends JPanel {
    private final WireBot botManager;
    private final ShellSender shellSender = new ShellSender(WireBot.getLogger());
    private final JFrame parent;


    public MainPanel(WireBot botManager, JFrame parent) {
        super();
        this.botManager = botManager;
        this.parent = parent;

        JPanel leftPanel = setLogPane();
        JPanel rightPanel = new RightPanelContainer(botManager, parent);

        setLayout(new BorderLayout());
        add(leftPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    private JPanel setLogPane() throws SecurityException {
        JPanel leftPanel = new JPanel();

        JScrollPane logPane = new JScrollPane();
        logPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        logPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JTextArea logArea = new JTextArea(10, 1);
        logArea.setEditable(false);

        logPane.setViewportView(logArea);

        WireBot.getLogger().addHandler(new LogHandler(logArea));

        JTextField commands = new JTextField();

        // commands.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.emptySet());

        commands.addActionListener(shellSender);
        commands.addKeyListener(new KeyAdapter() {
            private String cachedText = null;

            @Override
            public void keyPressed(KeyEvent e) {
                if (shellSender.getPointer() == -1) {
                    cachedText = commands.getText();
                }

                int pointer = shellSender.getPointer();
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        if (pointer < shellSender.getCommandHistory().size() - 1) {
                            shellSender.setPointer(pointer + 1);
                            commands.setText(shellSender.getCommandHistory().get(shellSender.getPointer()));
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                        if (pointer > -1) {
                            shellSender.setPointer(pointer - 1);

                            if (shellSender.getPointer() == -1) {
                                commands.setText(cachedText);
                            } else {
                                commands.setText(shellSender.getCommandHistory().get(shellSender.getPointer()));
                            }
                        } else {
                            commands.setText(cachedText);
                        }
                        break;
                    case KeyEvent.VK_ENTER:
                        cachedText = null;
                        break;
                        /*
                    case KeyEvent.VK_TAB:
                        e.consume();
                        ParseResults<ShellSender> results = shellSender.getDispatcher().parse(commands.getText(), shellSender);

                        System.out.println(results.getContext().findSuggestionContext(commands.getCaretPosition()).startPos);
                        System.out.println(results.getContext().findSuggestionContext(commands.getCaretPosition()).parent.getName());
                        break;*/
                }
            }
        });

        new GhostText(commands, "Type WireBot commands here...");

        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(logPane, BorderLayout.CENTER);
        leftPanel.add(commands, BorderLayout.SOUTH);

        return leftPanel;
    }
}
