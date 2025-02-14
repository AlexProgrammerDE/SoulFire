plugins {
  idea
  `java-library`
  id("sf-license-conventions")
  id("sf-formatting-conventions")
  id("io.freefair.lombok")
  id("net.kyori.indra.git")
  id("io.freefair.javadoc-utf-8")
}

tasks {
  javadoc {
    title = "SoulFire Javadocs"
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
    reports.junitXml.required = true
    reports.html.required = true
    useJUnitPlatform()
    maxParallelForks = Runtime.getRuntime().availableProcessors().div(2).coerceAtLeast(1)
  }
  withType<Jar> {
    from(rootProject.file("LICENSE"))
  }
  delombok {
    sourcepath.setFrom(sourcepath.plus(compileJava.get().options.generatedSourceOutputDirectory))
  }
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
  withJavadocJar()
  withSourcesJar()
}

afterEvaluate {
  tasks.withType<Zip> {
    isZip64 = true
  }
}
