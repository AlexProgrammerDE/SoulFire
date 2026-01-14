plugins {
  `sf-project-conventions`
}

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  api(projects.shared)
}

val modProjectName = ":mod"
evaluationDependsOn(modProjectName)
afterEvaluate {
  val modJarConfiguration = project(modProjectName).configurations.named("mod-jar")

  tasks.named<Jar>("jar") {
    from({
      modJarConfiguration.get().artifacts.files
    }) {
      into("META-INF/jars")
    }

    dependsOn(modJarConfiguration)
  }
}
