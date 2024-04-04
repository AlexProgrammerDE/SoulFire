package com.soulfiremc.launcher;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.HyperlinkEvent;

// Too early to init loggers
@SuppressWarnings("CallToPrintStackTrace")
public abstract class SoulFireAbstractJava8Launcher {
  public void run(String[] args) {
    try {
      Class.forName(getLauncherClassName())
        .getMethod("main", String[].class)
        .invoke(null, (Object) args);
    } catch (UnsupportedClassVersionError e) {
      System.out.println("[SoulFire] SoulFire requires Java 21 or higher!");
      System.out.println("[SoulFire] Please update your Java version!");
      System.out.println(
        "[SoulFire] You are currently using Java " + System.getProperty("java.version"));
      System.out.println(
        "[SoulFire] You can download the latest version of Java at https://adoptopenjdk.net/");

      if (!GraphicsEnvironment.isHeadless() && args.length == 0) {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e2) {
          throw new RuntimeException(e2);
        }
        new UnsupportedVersionDialog();
      }
    } catch (ClassNotFoundException e) {
      System.out.println("SoulFireLauncher is not in the classpath!");
      e.printStackTrace();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract String getLauncherClassName();

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
      pane.setText(
        "<html><center><h1>Unsupported Java Version</h1><br>"
          + "<h2>SoulFire requires Java 21 or higher!</h2><br>"
          + "<h2>Please update your Java version!</h2><br>"
          + "<h2>You are currently using Java "
          + System.getProperty("java.version")
          + "</h2><br>"
          +
          "<h2>You can download the latest version of Java at <a href=\"https://adoptium.net/\">https://adoptium.net/</a></h2></center></html>");
      pane.setEditable(false);
      pane.setBackground(null);
      pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      pane.addHyperlinkListener(
        event -> {
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
