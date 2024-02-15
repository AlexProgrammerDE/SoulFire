plugins {
    id("org.cadixdev.licenser")
}

license.header(rootProject.file("file_header.txt"))
license.newLine(false)
license.exclude("**/generated/**")

tasks.findByName("run")?.dependsOn("licenseFormat")
