plugins {
    java
}

java {
    javaTarget(8)
}

dependencies {
    implementation(projects.j8Launcher.common)
}
