plugins {
  `sf-project-conventions`
}

// Rename all artifacts
tasks.withType<AbstractArchiveTask> {
  if (archiveBaseName.isPresent && archiveBaseName.get() == "client") {
    archiveBaseName.set("SoulFireCLI")
  }
}

val projectMainClass = "com.soulfiremc.launcher.SoulFireCLIJava8Launcher"

tasks.register("runSFCLI", JavaExec::class) {
  group = "application"
  description = "Runs the SoulFire client"

  mainClass = projectMainClass
  classpath = sourceSets["main"].runtimeClasspath

  val argsMutable = mutableListOf(
    "-Xmx2G",
    "-XX:+EnableDynamicAgentLoading",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+UseZGC",
    "-XX:+ZGenerational",
    "-XX:+AlwaysActAsServerClassMachine",
    "-XX:+UseNUMA",
    "-XX:+UseFastUnorderedTimeStamps",
    "-XX:+UseVectorCmov",
    "-XX:+UseCriticalJavaThreadPriority",
    "-Dsf.flags.v1=true"
  )

  if (System.getProperty("idea.active") != null) {
    argsMutable += "-Dnet.kyori.ansi.colorLevel=truecolor"
  }

  jvmArgs = argsMutable

  standardInput = System.`in`

  outputs.upToDateWhen { false }
}

tasks.register("printSFCliFlags", JavaExec::class) {
  group = "application"
  description = "Runs the SoulFire client"

  mainClass = projectMainClass
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf(
    "--generate-flags"
  )

  val argsMutable = mutableListOf(
    "-Xmx2G",
    "-XX:+EnableDynamicAgentLoading",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+UseZGC",
    "-XX:+ZGenerational",
    "-XX:+AlwaysActAsServerClassMachine",
    "-XX:+UseNUMA",
    "-XX:+UseFastUnorderedTimeStamps",
    "-XX:+UseVectorCmov",
    "-XX:+UseCriticalJavaThreadPriority",
    "-Dsf.flags.v1=true"
  )

  if (System.getProperty("idea.active") != null) {
    argsMutable += "-Dnet.kyori.ansi.colorLevel=truecolor"
  }

  jvmArgs = argsMutable

  standardInput = System.`in`

  outputs.upToDateWhen { false }
}

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  implementation(projects.buildData)
  api(projects.proto)
  api(projects.mod)

  // The java 8 launcher takes care of notifying the user if they are using an unsupported java version
  implementation(projects.j8Launcher)

  // For CLI support
  api(libs.picoli)
  annotationProcessor(libs.picoli.codegen)
}

fun Manifest.applySFAttributes() {
  attributes["Main-Class"] = projectMainClass
  attributes["Name"] = "SoulFire"
  attributes["Specification-Title"] = "SoulFire"
  attributes["Specification-Version"] = version.toString()
  attributes["Specification-Vendor"] = "AlexProgrammerDE"
  attributes["Implementation-Title"] = "SoulFire"
  attributes["Implementation-Version"] = version.toString()
  attributes["Implementation-Vendor"] = "AlexProgrammerDE"
  attributes["Multi-Release"] = "true"
}

tasks {
  jar {
    archiveClassifier = "unshaded"

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(configurations.runtimeClasspath)
    from({
      configurations.runtimeClasspath.get()
        .filter { it.name.endsWith("jar") }
        .filter { it.toString().contains("build" + File.separator + "libs") }
        .map { zipTree(it) }
    })

    manifest.applySFAttributes()
  }
  val uberJar = register<Jar>("uberJar") {
    dependsOn(jar)
    from(zipTree(jar.get().outputs.files.singleFile))

    manifest.applySFAttributes()

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(configurations.runtimeClasspath)
    from({
      configurations.runtimeClasspath.get()
        .filter { it.name.endsWith("jar") }
        .filter { !it.toString().contains("build" + File.separator + "libs") }
    }) {
      into("META-INF/lib")
    }
  }
  build {
    dependsOn(uberJar)
  }
}
