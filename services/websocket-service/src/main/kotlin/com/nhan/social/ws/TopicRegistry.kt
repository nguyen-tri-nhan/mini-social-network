package com.nhan.social.ws

import io.quarkus.websockets.next.WebSocketConnection
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

@ApplicationScoped
class TopicRegistry {

    private val registry = ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketConnection>>()

    fun subscribe(topic: String, conn: WebSocketConnection) {
        registry.getOrPut(topic) { CopyOnWriteArraySet() }.add(conn)
    }

    fun unsubscribe(topic: String, conn: WebSocketConnection) {
        registry[topic]?.remove(conn)
    }

    fun unsubscribeAll(conn: WebSocketConnection) {
        registry.values.forEach { it.remove(conn) }
    }

    fun getConnections(topic: String): Set<WebSocketConnection> =
        registry[topic] ?: emptySet()
}
