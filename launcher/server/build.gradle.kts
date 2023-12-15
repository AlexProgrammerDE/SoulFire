plugins {
    java
}

java {
    javaTarget(8)
}

dependencies {
    implementation(projects.launcher.common)
}
