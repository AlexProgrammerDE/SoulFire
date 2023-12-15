package net.pistonmaster.serverwrecker.launcher;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;

public class ServerWreckerJava8Launcher {
    public static void main(String[] args) {
        try {
            Class.forName("net.pistonmaster.serverwrecker.ServerWreckerLauncher")
                    .getMethod("main", String[].class)
                    .invoke(null, (Object) args);
        } catch (UnsupportedClassVersionError e) {
            System.out.println("[ServerWrecker] ServerWrecker requires Java 21 or higher!");
            System.out.println("[ServerWrecker] Please update your Java version!");
            System.out.println("[ServerWrecker] You are currently using Java " + System.getProperty("java.version"));
            System.out.println("[ServerWrecker] You can download the latest version of Java at https://adoptopenjdk.net/");

            if (!GraphicsEnvironment.isHeadless()) {
                new UnsupportedVersionDialog();
            }
        } catch (ReflectiveOperationException e) {
            System.out.println("ServerWreckerLauncher is not in the classpath!");
            e.printStackTrace();
        }
    }

    private static class UnsupportedVersionDialog extends JFrame {
        public UnsupportedVersionDialog() {
            super("Unsupported Java Version");
            setResizable(false);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setSize(400, 200);
            setLocationRelativeTo(null);
            JTextPane pane = new JTextPane();
            pane.setContentType("text/html");
            pane.setText("<html><center><h1>Unsupported Java Version</h1><br>" +
                    "<h2>ServerWrecker requires Java 21 or higher!</h2><br>" +
                    "<h2>Please update your Java version!</h2><br>" +
                    "<h2>You are currently using Java " + System.getProperty("java.version") + "</h2><br>" +
                    "<h2>You can download the latest version of Java at <a href=\"https://adoptium.net/\">https://adoptium.net/</a></h2></center></html>"
            );
            pane.setEditable(false);
            pane.setBackground(null);
            pane.setBorder(null);
            pane.addHyperlinkListener(event -> {
                if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED
                        && Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(event.getURL().toURI());
                    } catch (IOException | URISyntaxException e) {
                        System.out.println("Failed to open link!");
                        e.printStackTrace();
                    }
                }
            });
            add(pane);
            pack();

            setVisible(true);
        }
    }
}
