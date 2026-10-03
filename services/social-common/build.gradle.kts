plugins {
    kotlin("jvm")
}

dependencies {
    implementation(platform(project(":social-bom")))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("cz.jirutka.rsql:rsql-parser")

    testImplementation(platform(project(":social-bom")))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

