plugins {
    id("com.diffplug.spotless")
}

spotless {
    java {
        trimTrailingWhitespace()
        indentWithSpaces(4)
        endWithNewline()

        palantirJavaFormat()

        importOrder("", "java|javax", "\\#")
    }
}
