plugins {
    `sf-java-conventions`
}

dependencies {
    implementation(projects.buildData)
    implementation(projects.proto)

    // Log/Console libraries
    implementation(libs.bundles.log4j)
    implementation(libs.jline)
    implementation(libs.jansi)
    implementation(libs.bundles.ansi4j)
    implementation(libs.terminalconsoleappender)
    api(libs.slf4j)
    implementation(libs.disruptor)

    // For command handling
    api(libs.brigadier)

    api(libs.bundles.kyori)

    api(libs.commons.validator)
    api(libs.commons.io)

    api(libs.guava)
    api(libs.gson)
    api(libs.pf4j) {
        isTransitive = false
    }
    api(libs.fastutil)
    api(libs.caffeine)

    api(libs.bundles.mixins)
    api(libs.reflect)
    api(libs.lambdaevents)

    // For detecting the dir to put data in
    implementation(libs.appdirs)

    // For class injection
    api(libs.injector)
}
