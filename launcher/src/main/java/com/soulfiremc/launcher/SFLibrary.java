package com.soulfiremc.launcher;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.LibClassifier;

public enum SFLibrary implements LibClassifier.LibraryType {
  JLINE_TERMINAL("org/jline/terminal/Terminal.class"),
  JLINE_READER("org/jline/reader/LineReader.class");

  public static final SFLibrary[] LOGGING = {
    JLINE_TERMINAL,
    JLINE_READER
  };

  private final EnvType env;
  private final String[] paths;

  SFLibrary(String path) {
    this(null, new String[] { path });
  }

  SFLibrary(String... paths) {
    this(null, paths);
  }

  SFLibrary(EnvType env, String... paths) {
    this.paths = paths;
    this.env = env;
  }

  @Override
  public boolean isApplicable(EnvType env) {
    return this.env == null || this.env == env;
  }

  @Override
  public String[] getPaths() {
    return paths;
  }
}
