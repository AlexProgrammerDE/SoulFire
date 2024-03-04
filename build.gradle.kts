plugins {
    base
}

val mavenVersion: String by project

allprojects {
    group = "com.soulfiremc"
    version = mavenVersion
    description = "Advanced Minecraft Server-Stresser Tool."
}
