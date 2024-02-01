package net.pistonmaster.soulfire.launcher;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;

// Too early to init loggers
@SuppressWarnings("CallToPrintStackTrace")
public class SoulFireJava8Launcher {
    public static void main(String[] args) {
        try {
            Class.forName("net.pistonmaster.soulfire.SoulFireLauncher")
                    .getMethod("main", String[].class)
                    .invoke(null, (Object) args);
        } catch (UnsupportedClassVersionError e) {
            System.out.println("[SoulFire] SoulFire requires Java 21 or higher!");
            System.out.println("[SoulFire] Please update your Java version!");
            System.out.println("[SoulFire] You are currently using Java " + System.getProperty("java.version"));
            System.out.println("[SoulFire] You can download the latest version of Java at https://adoptopenjdk.net/");

            if (!GraphicsEnvironment.isHeadless() && args.length == 0) {
                FlatDarkLaf.setup();
                new UnsupportedVersionDialog();
            }
        } catch (ReflectiveOperationException e) {
            System.out.println("SoulFireLauncher is not in the classpath!");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
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
                    "<h2>SoulFire requires Java 21 or higher!</h2><br>" +
                    "<h2>Please update your Java version!</h2><br>" +
                    "<h2>You are currently using Java " + System.getProperty("java.version") + "</h2><br>" +
                    "<h2>You can download the latest version of Java at <a href=\"https://adoptium.net/\">https://adoptium.net/</a></h2></center></html>"
            );
            pane.setEditable(false);
            pane.setBackground(null);
            pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
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
