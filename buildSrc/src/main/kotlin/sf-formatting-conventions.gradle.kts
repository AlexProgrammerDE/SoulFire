plugins {
  id("com.diffplug.spotless")
}

spotless {
  java {
    target("src/**/java/com/soulfiremc/**/*.java")

    trimTrailingWhitespace()
    leadingTabsToSpaces(2)
    endWithNewline()

    importOrder("", "javax|java", "\\#")

    licenseHeaderFile(rootProject.file("file_header.txt"))
  }
}
