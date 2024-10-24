import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.filter.ReduceDuplicateLicensesFilter
import com.github.jk1.license.render.CsvReportRenderer
import com.github.jk1.license.render.InventoryHtmlReportRenderer

plugins {
  `sf-project-conventions`
  alias(libs.plugins.license.report)
}

// Rename all artifacts
tasks.withType<AbstractArchiveTask> {
  if (archiveBaseName.isPresent && archiveBaseName.get() == "dedicated") {
    archiveBaseName.set("SoulFireDedicated")
  }
}

val projectMainClass = "com.soulfiremc.launcher.SoulFireDedicatedJava8Launcher"

task("runSFDedicated", JavaExec::class) {
  group = "application"
  description = "Runs the SoulFire dedicated server"

  mainClass = projectMainClass
  classpath = sourceSets["main"].runtimeClasspath

  jvmArgs = listOf(
    "-Xmx18G",
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

  standardInput = System.`in`

  val runDir = projectDir.resolve("run")
  runDir.mkdirs()
  workingDir = runDir

  outputs.upToDateWhen { false }
}

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  implementation(projects.buildData)
  api(projects.proto)
  api(projects.server)

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
}

tasks {
  jar {
    archiveClassifier = "unshaded"

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(configurations.runtimeClasspath)
    from({
      configurations.runtimeClasspath.get()
        .filter { it.name.endsWith("jar") }
        .filter { it.toString().contains("build/libs") }
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
        .filter { !it.toString().contains("build/libs") }
    }) {
      into("META-INF/lib")
    }
  }
  build {
    dependsOn(uberJar)
  }
}

licenseReport {
  projects = arrayOf(rootProject)

  renderers = arrayOf(InventoryHtmlReportRenderer(), CsvReportRenderer())
  filters = arrayOf(LicenseBundleNormalizer(), ReduceDuplicateLicensesFilter())

  excludeBoms = true
}
