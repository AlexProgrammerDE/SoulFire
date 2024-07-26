package com.soulfiremc.launcher;

public class SoulFireCLIJava8Launcher extends SoulFireAbstractJava8Launcher {
  public static void main(String[] args) {
    new SoulFireCLIJava8Launcher().run(args);
  }

  @Override
  protected String getLauncherClassName() {
    return "com.soulfiremc.client.SoulFireCLILauncher";
  }
}
