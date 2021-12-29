plugins {
    id("sw.java-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.10-SNAPSHOT")
    compileOnly(projects.serverwreckerCommon)
}
