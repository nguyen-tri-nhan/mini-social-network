rootProject.name = "social-services"

include(
    // Shared libs
    "social-common",
    "social-exception",

    // Auth (REST only — no consumer)
    "auth-service-dao",
    "auth-service",

    // User
    "user-service-dao",
    "user-service",         // shared logic lib
    "user-api",             // Quarkus: REST
    "user-consumer",        // Quarkus: Kafka consumer

    // Post
    "post-service-dao",
    "post-service",         // shared logic lib
    "post-api",             // Quarkus: REST
    "post-consumer",        // Quarkus: Kafka consumer + counter flush

    // Interaction (REST + Kafka publish — no consumer)
    "interaction-service-dao",
    "interaction-service",

    // Notification
    "notification-service-dao",
    "notification-service",      // shared logic lib
    "notification-api",          // Quarkus: REST
    "notification-consumer",     // Quarkus: Kafka consumer
)
