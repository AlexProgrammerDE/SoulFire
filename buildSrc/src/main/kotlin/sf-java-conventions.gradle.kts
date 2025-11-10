plugins {
  idea
  `java-library`
  id("sf-formatting-conventions")
  id("io.freefair.lombok")
  id("net.kyori.indra.git")
  id("io.freefair.javadoc-utf-8")
  id("net.ltgt.errorprone")
  id("com.github.spotbugs")
  id("org.openrewrite.rewrite")
}

rewrite {
  activeRecipe("org.openrewrite.staticanalysis.CodeCleanup")
  activeRecipe("org.openrewrite.java.migrate.UpgradeToJava25")
  activeRecipe("org.openrewrite.java.recipes.RecipeTestingBestPractices")
  isExportDatatables = true
}

spotbugs {
  ignoreFailures = true
}

dependencies {
  errorprone("com.google.errorprone:error_prone_core:2.44.0")
  spotbugs("com.github.spotbugs:spotbugs:4.9.8")
  rewrite("org.openrewrite.recipe:rewrite-static-analysis:2.20.0")
  rewrite("org.openrewrite.recipe:rewrite-migrate-java:3.20.0")
  rewrite("org.openrewrite.recipe:rewrite-rewrite:0.14.1")
}

tasks {
  // Variable replacements
  processResources {
    filesMatching(listOf("fabric.mod.json", "soulfire-build-data.properties")) {
      expand(
        mapOf(
          "version" to project.version,
          "description" to project.description,
          "url" to "https://soulfiremc.com",
          "commit" to (indraGit.commit().orNull?.name ?: "unknown"),
          "branch" to (indraGit.branchName().orNull ?: "unknown"),
        )
      )
    }
  }
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
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
  withJavadocJar()
  withSourcesJar()
}

lombok {
  version = "1.18.42"
}

afterEvaluate {
  tasks.withType<Zip> {
    isZip64 = true
  }
}
