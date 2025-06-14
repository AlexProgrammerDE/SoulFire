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
  val remappedConfiguration = project(modProjectName).configurations.named("remapped")

  tasks.named<Jar>("jar") {
    from({
      remappedConfiguration.get().artifacts.files
    }) {
      into("META-INF/jars")
    }

    dependsOn(remappedConfiguration)
  }
}
