package com.nhan.social.ws

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger

@ApplicationScoped
class WsPushService(
    private val topicRegistry: TopicRegistry,
    private val objectMapper: ObjectMapper,
) {
    private val log = Logger.getLogger(WsPushService::class.java)

    fun push(topic: String, message: ServerMessage) {
        val json = objectMapper.writeValueAsString(message)
        topicRegistry.getConnections(topic).forEach { conn ->
            conn.sendText(json)
                .subscribe().with(
                    { /* sent */ },
                    { e -> log.warnf("Send failed on topic %s: %s", topic, e.message) },
                )
        }
        // TODO Phase 2: publish to Redis pub/sub for multi-instance fan-out
        // redis.pubsub(String::class.java).publish("ws:topic:$topic", json)
    }
}
