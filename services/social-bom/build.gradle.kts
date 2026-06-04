plugins {
    `java-platform`
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    // Manages quarkus-amazon-s3 + all software.amazon.awssdk:* versions
    api(platform("io.quarkus.platform:quarkus-amazon-services-bom:3.20.1"))

    constraints {
        api("cz.jirutka.rsql:rsql-parser:2.1.0")
        api("at.favre.lib:bcrypt:0.10.2")
    }
}
