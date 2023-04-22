plugins {
    id("org.cadixdev.licenser")
}

license.header(rootProject.file("file_header.txt"))
license.newLine(false)

tasks.named("run").configure {
    dependsOn("checkLicenses")
}
