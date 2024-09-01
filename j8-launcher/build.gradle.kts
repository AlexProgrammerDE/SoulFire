plugins {
  idea
  java
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}
