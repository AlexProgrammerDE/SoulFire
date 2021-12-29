plugins {
    id("net.pistonmaster.java-conventions")
}

dependencies {
    implementation("com.github.Steveice10:mcauthlib:1.3")
    implementation("com.github.steveice10:packetlib:1.8")
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
    compileOnly(projects.serverwreckerCommon)
}
