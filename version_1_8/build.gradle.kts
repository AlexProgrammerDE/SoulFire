plugins {
    id("sw.java-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.8-SNAPSHOT")
    compileOnly(projects.serverwreckerCommon)
}
