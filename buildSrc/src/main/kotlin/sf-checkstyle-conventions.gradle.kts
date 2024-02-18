plugins {
    checkstyle
}

extensions.configure<CheckstyleExtension> {
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    configDirectory = rootProject.file("config/checkstyle")
    maxErrors = 0
    maxWarnings = 0
    toolVersion = libs.checkstyle.get().version.toString()
}
