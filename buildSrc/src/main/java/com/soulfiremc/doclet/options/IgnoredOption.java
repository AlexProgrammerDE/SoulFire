package com.soulfiremc.doclet.options;

import jdk.javadoc.doclet.Doclet;

import java.util.List;

public class IgnoredOption implements Doclet.Option {
  private final String name;
  private final int params;

  public IgnoredOption(String name, int params) {
    this.name = name;
    this.params = params;
  }

  @Override
  public int getArgumentCount() {
    return params;
  }

  @Override
  public String getDescription() {
    return "";
  }

  @Override
  public Kind getKind() {
    return Kind.STANDARD;
  }

  @Override
  public List<String> getNames() {
    return List.of(name);
  }

  @Override
  public String getParameters() {
    return "";
  }

  @Override
  public boolean process(String option, List<String> arguments) {
    return true;
  }
}
