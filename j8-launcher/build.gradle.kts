plugins {
    java
}

dependencies {
    implementation(libs.flatlaf)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}
