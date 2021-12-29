plugins {
    id("sw.java-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.12-SNAPSHOT")
    compileOnly(projects.serverwreckerCommon)
}
