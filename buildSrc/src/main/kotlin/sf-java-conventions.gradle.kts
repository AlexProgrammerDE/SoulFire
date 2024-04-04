plugins {
  idea
  `java-library`
  id("sf-license-conventions")
  id("sf-checkstyle-conventions")
  id("io.freefair.lombok")
  id("net.kyori.indra.git")
}

tasks {
  javadoc {
    title = "SoulFire Javadocs"
    options.encoding = Charsets.UTF_8.name()
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
  }
  compileJava {
    options.encoding = Charsets.UTF_8.name()
    options.compilerArgs.addAll(
      listOf(
        "-parameters",
        "-nowarn",
        "-Xlint:-unchecked",
        "-Xlint:-deprecation",
        "-Xlint:-processing"
      )
    )
    options.isFork = true
  }
  test {
    useJUnitPlatform()
  }
  jar {
    from(rootProject.file("LICENSE"))
  }
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
  withJavadocJar()
  withSourcesJar()
}
