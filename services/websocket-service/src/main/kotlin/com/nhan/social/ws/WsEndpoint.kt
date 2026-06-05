package com.nhan.social.ws

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.OnOpen
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.WebSocket
import io.quarkus.websockets.next.WebSocketConnection
import org.jboss.logging.Logger

@WebSocket(path = "/ws")
class WsEndpoint(
    private val topicRegistry: TopicRegistry,
    private val objectMapper: ObjectMapper,
) {
    private val log = Logger.getLogger(WsEndpoint::class.java)

    @OnOpen
    fun onOpen(conn: WebSocketConnection) {
        log.debugf("Connection opened: %s", conn.id())
    }

    @OnTextMessage
    fun onMessage(message: String, conn: WebSocketConnection) {
        try {
            val msg = objectMapper.readValue(message, ClientMessage::class.java)
            when (msg.type.uppercase()) {
                "SUBSCRIBE"   -> {
                    topicRegistry.subscribe(msg.topic, conn)
                    log.debugf("Connection %s subscribed to %s", conn.id(), msg.topic)
                }
                "UNSUBSCRIBE" -> {
                    topicRegistry.unsubscribe(msg.topic, conn)
                    log.debugf("Connection %s unsubscribed from %s", conn.id(), msg.topic)
                }
                else -> log.warnf("Unknown message type: %s", msg.type)
            }
        } catch (e: Exception) {
            log.warnf("Failed to parse message from %s: %s", conn.id(), message)
        }
    }

    @OnClose
    fun onClose(conn: WebSocketConnection) {
        topicRegistry.unsubscribeAll(conn)
        log.debugf("Connection closed: %s", conn.id())
    }
}
