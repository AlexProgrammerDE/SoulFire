plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("gradle.plugin.com.github.johnrengelman:shadow:8.0.0")
    implementation("gradle.plugin.org.cadixdev.gradle:licenser:0.6.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.maven:maven-model:4.0.0-alpha-7")
}
