plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.gradle.plugin.shadow)
    implementation(libs.gradle.plugin.licenser)
    implementation(libs.gradle.plugin.indra)
    implementation(libs.gradle.plugin.lombok)
}
