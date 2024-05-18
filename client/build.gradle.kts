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
  if (archiveBaseName.isPresent && archiveBaseName.get() == "client") {
    archiveBaseName.set("SoulFireClient")
  }
}

val projectMainClass = "com.soulfiremc.launcher.SoulFireClientJava8Launcher"

task("runSFClient", JavaExec::class) {
  group = "application"
  description = "Runs the SoulFire client"

  mainClass = projectMainClass
  classpath = sourceSets["main"].runtimeClasspath

  jvmArgs = listOf(
    "-Xmx2G",
    "-XX:+EnableDynamicAgentLoading",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+UseG1GC",
    "-XX:G1NewSizePercent=20",
    "-XX:G1ReservePercent=20",
    "-XX:MaxGCPauseMillis=50",
    "-XX:G1HeapRegionSize=32M"
  )

  standardInput = System.`in`

  outputs.upToDateWhen { false }
}

task("runSFClientLocal", JavaExec::class) {
  group = "application"
  description = "Runs the SoulFire client"

  mainClass = projectMainClass
  classpath = sourceSets["main"].runtimeClasspath

  jvmArgs = listOf(
    "-Xmx2G",
    "-XX:+EnableDynamicAgentLoading",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+UseG1GC",
    "-XX:G1NewSizePercent=20",
    "-XX:G1ReservePercent=20",
    "-XX:MaxGCPauseMillis=50",
    "-XX:G1HeapRegionSize=32M",
    "-Dsf.disableServerSelect=true"
  )

  standardInput = System.`in`

  outputs.upToDateWhen { false }
}

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  implementation(projects.buildData)
  api(projects.proto)
  api(projects.common)
  api(projects.server)

  // The java 8 launcher takes care of notifying the user if they are using an unsupported java version
  implementation(projects.j8Launcher)

  // For CLI support
  api(libs.picoli)
  annotationProcessor(libs.picoli.codegen)

  // For GUI support
  api(libs.bundles.flatlaf)
  api(libs.xchart) {
    exclude("org.junit.jupiter")
  }
  api(libs.miglayout.swing)
  api(libs.commons.swing)

  val lwjglVersion = "3.3.3"
  val lwjglPlatforms = listOf("linux", "macos", "macos-arm64", "windows")
  lwjglPlatforms.forEach { platform ->
    api("org.lwjgl:lwjgl-nfd:$lwjglVersion:natives-$platform")
    api("org.lwjgl:lwjgl:$lwjglVersion:natives-$platform")
  }
  api("org.lwjgl:lwjgl-nfd:$lwjglVersion")
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
