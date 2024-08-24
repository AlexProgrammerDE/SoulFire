plugins {
  id("com.diffplug.spotless")
}

afterEvaluate {
  if (project.name != "proto") {
    spotless {
      java {
        trimTrailingWhitespace()
        indentWithSpaces(2)
        endWithNewline()

        importOrder("", "javax|java", "\\#")
      }
    }
  }
}
