plugins {
  `sf-project-conventions`
}

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  api(projects.shared)
  runtimeOnly(project(":mod", "remapped"))
}
