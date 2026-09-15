plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    // NO quarkus plugin — this is a shared lib
}

dependencies {
    api(platform(project(":social-bom")))
    // Chỉ module này + post-api cần S3 → import platform riêng ở đây, không
    // qua social-bom (xem specs/decisions/0002). Đã thử đổi sang BOM native
    // io.quarkiverse.amazonservices (né được lỗi "platform stream") nhưng lộ
    // ra conflict version thật ở quarkus-core/bootstrap — vẫn dùng bản mirror
    // io.quarkus.platform này, module vẫn KHÔNG build (JIB) được cho tới khi
    // quarkiverse-amazon-services release bản tương thích thật với
    // quarkus-bom:3.25.1 (xem specs/decisions/0002, 0003)
    api(platform("io.quarkus.platform:quarkus-amazon-services-bom:3.20.1"))
    implementation(project(":social-common"))
    implementation(project(":social-exception"))
    implementation(project(":post-service-dao"))

    implementation("jakarta.validation:jakarta.validation-api")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-redis-client")
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-s3")
    implementation("software.amazon.awssdk:url-connection-client")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.13.10")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

allOpen {
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.enterprise.context.RequestScoped")
    annotation("jakarta.persistence.Entity")
}

