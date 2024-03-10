package com.soulfiremc.launcher;

public class SoulFireDedicatedServerJava8Launcher extends SoulFireAbstractJava8Launcher {
  public static void main(String[] args) {
    new SoulFireDedicatedServerJava8Launcher().run(args);
  }

  @Override
  protected String getLauncherClassName() {
    return "com.soulfiremc.client.SoulFireClientLauncher";
  }
}
