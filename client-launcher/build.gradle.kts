plugins {
  `sf-project-conventions`
}

base {
  archivesName = "SoulFireCLI"
}

val projectMainClass = "com.soulfiremc.launcher.SoulFireCLIJava8Launcher"

tasks.register("runSFCLI", JavaExec::class) {
  group = "application"
  description = "Runs the SoulFire client"

  javaLauncher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(25)
  }

  mainClass = projectMainClass
  classpath = sourceSets["main"].runtimeClasspath

  val argsMutable = mutableListOf(
    "--enable-native-access=ALL-UNNAMED", // Needed for JavaExec
    "-Xmx8G",
    "-XX:+EnableDynamicAgentLoading",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+UseZGC",
    "-XX:+UseCompactObjectHeaders",
    "-XX:+AlwaysActAsServerClassMachine",
    "-XX:+UseNUMA",
    "-XX:+UseFastUnorderedTimeStamps",
    "-XX:+UseVectorCmov",
    "-XX:+UseCriticalJavaThreadPriority",
    "-Dsf.flags.v2=true",
    "-Dsf.remapToNamed=true"
  )

  if (System.getProperty("idea.active") != null) {
    argsMutable += "-Dnet.kyori.ansi.colorLevel=truecolor"
  }

  jvmArgs = argsMutable

  standardInput = System.`in`

  outputs.upToDateWhen { false }
}

tasks.register("printSFCLIFlags", JavaExec::class) {
  group = "application"
  description = "Runs the SoulFire client"

  javaLauncher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(25)
  }

  mainClass = projectMainClass
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf(
    "--generate-flags"
  )

  val argsMutable = mutableListOf(
    "--enable-native-access=ALL-UNNAMED", // Needed for JavaExec
    "-Xmx2G",
    "-XX:+EnableDynamicAgentLoading",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+UseZGC",
    "-XX:+AlwaysActAsServerClassMachine",
    "-XX:+UseNUMA",
    "-XX:+UseFastUnorderedTimeStamps",
    "-XX:+UseVectorCmov",
    "-XX:+UseCriticalJavaThreadPriority",
    "-Dsf.flags.v2=true"
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

  api(projects.launcher)

  // The java 8 launcher takes care of notifying the user if they are using an unsupported java version
  implementation(projects.j8Launcher)
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
  attributes["Enable-Native-Access"] = "ALL-UNNAMED"
}

fun File.isJar(): Boolean {
  return name.endsWith(".jar")
}

fun File.shouldShadow(): Boolean {
  return toString().contains("launcher" + File.separator + "build" + File.separator + "libs")
}

tasks {
  val generateDependencyList = register("generateDependencyList") {
    dependsOn(configurations.runtimeClasspath)
    inputs.files(configurations.runtimeClasspath)

    val outputFile = layout.buildDirectory.file("dependency-list.txt")
    outputs.file(outputFile)

    doLast {
      val dependencies = configurations.runtimeClasspath.get().files
        .filter { it.isJar() }
        .filter { !it.shouldShadow() }
        .joinToString("\n") { it.name }
      outputFile.get().asFile.writeText(dependencies)
    }
  }
  jar {
    archiveClassifier = "unshaded"

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
        .filter { it.isJar() }
        .filter { it.shouldShadow() }
        .map { zipTree(it) }
    })
    from({
      configurations.runtimeClasspath.get()
        .filter { it.isJar() }
        .filter { !it.shouldShadow() }
    }) {
      into("META-INF/lib")
    }
    from(generateDependencyList.get().outputs.files) {
      into("META-INF")
    }
  }
  build {
    dependsOn(uberJar)
  }
}
