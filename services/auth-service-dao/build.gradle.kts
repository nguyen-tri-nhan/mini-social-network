plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
}

dependencies {
    api(platform(project(":social-bom")))
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-liquibase")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
}

