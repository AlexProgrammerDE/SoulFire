plugins {
  id("com.diffplug.spotless")
}

afterEvaluate {
  if (project.name != "proto") {
    spotless {
      java {
        trimTrailingWhitespace()
        leadingTabsToSpaces(2)
        endWithNewline()

        importOrder("", "javax|java", "\\#")
      }
    }
  }
}
