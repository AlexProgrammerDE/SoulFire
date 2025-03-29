package com.soulfiremc.doclet.options;

import jdk.javadoc.doclet.Doclet;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class OutputDirectoryOption implements Doclet.Option {
  public static File outputDir;

  @Override
  public int getArgumentCount() {
    return 1;
  }

  @Override
  public String getDescription() {
    return "set output directory";
  }

  @Override
  public Kind getKind() {
    return Kind.STANDARD;
  }

  @Override
  public List<String> getNames() {
    return List.of("-d", "--out");
  }

  @Override
  public String getParameters() {
    return "<outdir: File>";
  }

  @Override
  public boolean process(String option, List<String> arguments) {
    try {
      outputDir = new File(arguments.getFirst()).getCanonicalFile();
    } catch (IOException e) {
      return false;
    }
    return true;
  }
}
