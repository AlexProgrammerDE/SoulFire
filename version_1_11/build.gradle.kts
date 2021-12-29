plugins {
    id("sw.java-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.11-SNAPSHOT")
    compileOnly(projects.serverwreckerCommon)
}
