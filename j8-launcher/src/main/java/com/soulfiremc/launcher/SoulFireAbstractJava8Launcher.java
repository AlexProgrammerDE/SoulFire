package com.soulfiremc.launcher;

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
      System.out.println("[SoulFire] You are currently using Java " + System.getProperty("java.version"));
      System.out.println("[SoulFire] You can download the latest version of Java at https://adoptopenjdk.net/");
    } catch (ClassNotFoundException e) {
      System.out.println("SoulFireLauncher is not in the classpath!");
      e.printStackTrace();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract String getLauncherClassName();
}
