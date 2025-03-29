package com.soulfiremc.doclet;

import com.soulfiremc.doclet.options.IgnoredOption;
import com.soulfiremc.doclet.options.OutputDirectoryOption;
import com.soulfiremc.doclet.options.VersionOption;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.SourceVersion;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings("unused")
public class PyDoclet implements Doclet {
  public static Reporter reporter;

  @Override
  public void init(Locale locale, Reporter reporter) {
    PyDoclet.reporter = reporter;
  }

  @Override
  public String getName() {
    return "Python Generator";
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
    return true;
  }
}
