plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
}

dependencies {
    api(platform(project(":social-bom")))
    implementation(project(":social-common"))
    implementation(project(":social-exception"))
    implementation(project(":user-service-dao"))

    implementation("jakarta.validation:jakarta.validation-api")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-redis-client")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}

allOpen {
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.enterprise.context.RequestScoped")
    annotation("jakarta.persistence.Entity")
}

