package com.soulfiremc.doclet.options;

import jdk.javadoc.doclet.Doclet;

import java.util.List;

public class VersionOption implements Doclet.Option {
  public static String version = null;

  @Override
  public int getArgumentCount() {
    return 1;
  }

  @Override
  public String getDescription() {
    return "version info for building";
  }

  @Override
  public Kind getKind() {
    return Kind.STANDARD;
  }

  @Override
  public List<String> getNames() {
    return List.of("-v", "--version");
  }

  @Override
  public String getParameters() {
    return "<version: String>";
  }

  @Override
  public boolean process(String option, List<String> arguments) {
    version = arguments.getFirst();
    return true;
  }
}
