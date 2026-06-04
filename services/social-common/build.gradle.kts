plugins {
    kotlin("jvm")
}

dependencies {
    implementation(platform(project(":social-bom")))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("cz.jirutka.rsql:rsql-parser")
}

