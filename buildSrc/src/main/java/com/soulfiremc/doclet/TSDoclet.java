package com.soulfiremc.doclet;

import com.soulfiremc.doclet.options.IgnoredOption;
import com.soulfiremc.doclet.options.OutputDirectoryOption;
import com.soulfiremc.doclet.options.VersionOption;
import com.soulfiremc.doclet.tsdoclet.TypeScriptGenerator;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings("unused")
public class TSDoclet implements Doclet {
  public static Reporter reporter;

  @Override
  public void init(Locale locale, Reporter reporter) {
    TSDoclet.reporter = reporter;
  }

  @Override
  public String getName() {
    return "TypeScript Generator";
  }

  @SuppressWarnings("SpellCheckingInspection")
  @Override
  public Set<? extends Option> getSupportedOptions() {
    return Set.of(
      new VersionOption(),
      new OutputDirectoryOption(),
      new IgnoredOption("-doctitle", 1),
      new IgnoredOption("-notimestamp", 0),
      new IgnoredOption("-windowtitle", 1),
      new IgnoredOption("-charset", 1),
      new IgnoredOption("-docencoding", 1)
    );
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_21;
  }

  @Override
  public boolean run(DocletEnvironment environment) {
    try {
      var generator = new TypeScriptGenerator(environment);
      generator.generate();
      return true;
    } catch (Exception e) {
      reporter.print(Diagnostic.Kind.ERROR, "Failed to generate TypeScript definitions: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }
}
