plugins {
  `sf-project-conventions`
}

val modRemapped: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  api(projects.shared)

  modRemapped(project(":mod", "remapped"))
}

tasks.named<Jar>("jar") {
  from({
    modRemapped.resolve()
  }) {
    into("META-INF/jars")
  }
}
