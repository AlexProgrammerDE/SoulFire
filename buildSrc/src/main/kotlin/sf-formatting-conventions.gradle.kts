plugins {
    id("com.diffplug.spotless")
}

spotless {
    java {
        trimTrailingWhitespace()
        indentWithSpaces(2)
        endWithNewline()

        importOrder("", "javax|java", "\\#")
    }
}
