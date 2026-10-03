plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    // NO quarkus plugin — shared lib cho chat-api + websocket-service
}

dependencies {
    api(platform(project(":social-bom")))
    // Lib nào dùng AWS thì lib đó khai platform, app không khai lại — ADR 0004.
    api(platform("io.quarkus.platform:quarkus-amazon-services-bom:3.20.1"))

    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-kms")
    implementation("software.amazon.awssdk:url-connection-client")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.13.10")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

allOpen {
    annotation("jakarta.enterprise.context.ApplicationScoped")
}
