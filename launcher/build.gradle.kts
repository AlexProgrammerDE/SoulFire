plugins {
  `sf-project-conventions`
}

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  implementation(projects.buildData)
}
