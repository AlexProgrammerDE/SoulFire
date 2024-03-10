package com.soulfiremc.launcher;

public class SoulFireDedicatedJava8Launcher extends SoulFireAbstractJava8Launcher {
  public static void main(String[] args) {
    new SoulFireDedicatedJava8Launcher().run(args);
  }

  @Override
  protected String getLauncherClassName() {
    return "com.soulfiremc.dedicated.SoulFireDedicatedLauncher";
  }
}
