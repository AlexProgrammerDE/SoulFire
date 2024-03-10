package com.soulfiremc.launcher;

public class SoulFireClientJava8Launcher extends SoulFireAbstractJava8Launcher {
  public static void main(String[] args) {
    new SoulFireClientJava8Launcher().run(args);
  }

  @Override
  protected String getLauncherClassName() {
    return "com.soulfiremc.client.SoulFireClientLauncher";
  }
}
