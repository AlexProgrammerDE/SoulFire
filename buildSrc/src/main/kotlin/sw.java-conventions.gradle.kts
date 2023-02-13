plugins {
    `java-library`
    `maven-publish`
    id("sw.license-conventions")
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("org.fusesource.jansi:jansi:2.4.0")
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}
