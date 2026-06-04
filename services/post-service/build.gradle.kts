plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    // NO quarkus plugin — this is a shared lib
}

dependencies {
    api(platform(project(":social-bom")))
    implementation(project(":social-common"))
    implementation(project(":social-exception"))
    implementation(project(":post-service-dao"))

    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-redis-client")
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-s3")
    implementation("software.amazon.awssdk:url-connection-client")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}

allOpen {
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.enterprise.context.RequestScoped")
    annotation("jakarta.persistence.Entity")
}

