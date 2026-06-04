import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.0" apply false
    kotlin("plugin.allopen") version "2.0.0" apply false
    id("io.quarkus") version "3.25.1" apply false
}

val jvmTarget = project.properties["kotlin.jvm.target"] as? String ?: "21"

subprojects {
    if (name == "social-bom") return@subprojects

    repositories {
        mavenCentral()
        mavenLocal()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = jvmTarget
    }

    tasks.withType<Test>().configureEach {
        systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    }

    // Quarkus tạo riêng các config: integrationTest*, nativeTest*, quarkusGeneratedSources*
    // Chúng extend testImplementation nên cần BOM ở đây, không chỉ ở implementation
    plugins.withId("io.quarkus") {
        dependencies {
            add("testImplementation", enforcedPlatform(project(":social-bom")))
        }
    }
}

// ─── Liquibase migrations ─────────────────────────────────────────────────────

val liquibaseRuntime by configurations.creating

repositories {
    mavenCentral()
}

dependencies {
    liquibaseRuntime("org.liquibase:liquibase-core:4.29.2")
    liquibaseRuntime("org.postgresql:postgresql:42.7.7")
    liquibaseRuntime("info.picocli:picocli:4.7.6")
}

// key → (daoModule, dbName)
val allSchemas = mapOf(
    "auth"         to Pair("auth-service-dao",        "auth_db"),
    "user"         to Pair("user-service-dao",         "user_db"),
    "post"         to Pair("post-service-dao",         "post_db"),
    "interaction"  to Pair("interaction-service-dao",  "interaction_db"),
    "notification" to Pair("notification-service-dao", "notification_db"),
)

// SCHEMAS=auth,user  or empty → all
val schemasEnv = System.getenv("SCHEMAS")
val selectedKeys: Set<String> = if (schemasEnv.isNullOrBlank()) {
    allSchemas.keys
} else {
    schemasEnv.split(",").map { it.trim().lowercase() }.toSet()
}

val dbHost     = System.getenv("DB_HOST")     ?: "localhost"
val dbPort     = System.getenv("DB_PORT")     ?: "5432"
val dbUsername = System.getenv("DB_USERNAME") ?: "postgres"
val dbPassword = System.getenv("DB_PASSWORD") ?: "postgres"

// Individual task per schema (always registered so they can be called directly)
allSchemas.forEach { (key, config) ->
    val (daoModule, dbName) = config
    tasks.register<JavaExec>("liquibaseUpdate_$key") {
        group = "liquibase"
        description = "Migrate $key ($dbName)"
        classpath = liquibaseRuntime
        mainClass.set("liquibase.integration.commandline.LiquibaseCommandLine")

        val url = System.getenv("${key.uppercase()}_DB_URL")
            ?: "jdbc:postgresql://$dbHost:$dbPort/$dbName"
        val changelog = project(":$daoModule")
            .projectDir
            .resolve("src/main/resources/db/changelog/db.changelog-master.xml")
            .absolutePath

        args(
            "--url=$url",
            "--username=$dbUsername",
            "--password=$dbPassword",
            "--changelogFile=$changelog",
            "--driver=org.postgresql.Driver",
            "update",
        )
    }
}

tasks.register("liquibaseUpdate") {
    group = "liquibase"
    description = """
        Run Liquibase migrations.
        SCHEMAS=auth,user,post,interaction,notification  (comma-separated, default: all)
        DB_HOST / DB_PORT / DB_USERNAME / DB_PASSWORD    (default: localhost:5432 / postgres)
        AUTH_DB_URL / USER_DB_URL / ...                  (override full JDBC URL per schema)
    """.trimIndent()

    val targets = selectedKeys.filter { it in allSchemas }
    if (targets.isEmpty() && schemasEnv != null) {
        logger.warn("No matching schemas for '$schemasEnv'. Available: ${allSchemas.keys.joinToString()}")
    }

    dependsOn(targets.map { "liquibaseUpdate_$it" })
}
