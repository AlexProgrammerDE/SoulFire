plugins {
    `java-library`
    `maven-publish`
    id("sw.license-conventions")
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.20")
    annotationProcessor("org.projectlombok:lombok:1.18.20")
    compileOnly("ch.qos.logback:logback-classic:1.2.3")
}

java.sourceCompatibility = JavaVersion.VERSION_17

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}
