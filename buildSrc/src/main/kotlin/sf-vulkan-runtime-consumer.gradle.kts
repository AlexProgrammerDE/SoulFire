plugins {
  java
}

val vulkanRuntimeDependencies = configurations.dependencyScope("vulkanRuntimeDependencies")

// Native packages are bundled locally and have no Maven publication.
// Add them to execution classpaths without exporting a published dependency.
sourceSets.configureEach {
  configurations.named(runtimeClasspathConfigurationName) {
    extendsFrom(vulkanRuntimeDependencies.get())
  }
}

dependencies {
  add(vulkanRuntimeDependencies.name, project(":vulkan-runtime"))
}
