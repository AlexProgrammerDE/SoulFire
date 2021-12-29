plugins {
    id("sw.java-conventions")
}

dependencies {
    implementation("com.github.GeyserMC:mcauthlib:1.4")
    implementation("com.github.GeyserMC:packetlib:2.1")
    implementation(projects.serverwreckerVersion17)
    implementation(projects.serverwreckerVersion18)
    implementation(projects.serverwreckerVersion19)
    implementation(projects.serverwreckerVersion110)
    implementation(projects.serverwreckerVersion111)
    implementation(projects.serverwreckerVersion112)
    implementation(projects.serverwreckerVersion113)
    implementation(projects.serverwreckerVersion114)
    implementation(projects.serverwreckerVersion115)
    implementation(projects.serverwreckerVersion116)
    implementation(projects.serverwreckerVersion117)
    implementation(projects.serverwreckerVersion118)
    compileOnly(projects.serverwreckerCommon)
}
