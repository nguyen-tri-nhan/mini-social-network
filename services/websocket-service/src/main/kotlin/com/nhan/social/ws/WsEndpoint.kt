package com.nhan.social.ws

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.OnOpen
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.WebSocket
import io.quarkus.websockets.next.WebSocketConnection
import org.eclipse.microprofile.jwt.JsonWebToken
import org.jboss.logging.Logger

// Upgrade không có JWT hợp lệ bị từ chối ngay (@Authenticated). Browser gửi token
// qua Sec-WebSocket-Protocol — xem ADR 0009 và application.properties.
@Authenticated
@WebSocket(path = "/ws")
class WsEndpoint(
    private val topicRegistry: TopicRegistry,
    private val objectMapper: ObjectMapper,
    private val identity: SecurityIdentity,
) {
    private val log = Logger.getLogger(WsEndpoint::class.java)

    @OnOpen
    fun onOpen(conn: WebSocketConnection) {
        log.debugf("Connection opened: %s", conn.id())
    }

    @OnTextMessage
    fun onMessage(message: String, conn: WebSocketConnection) {
        val msg = try {
            objectMapper.readValue(message, ClientMessage::class.java)
        } catch (e: Exception) {
            log.warnf("Failed to parse message from %s: %s", conn.id(), message)
            return
        }

        when (msg.type.uppercase()) {
            "SUBSCRIBE" -> {
                // Dùng subject (userId) như mọi service khác — principal.name của MP JWT
                // lấy upn/preferred_username trước, không phải lúc nào cũng là userId.
                val userId = (identity.principal as? JsonWebToken)?.subject
                if (userId == null || !TopicAuthorizer.canSubscribe(msg.topic, userId)) {
                    log.debugf("Connection %s denied subscribe to %s", conn.id(), msg.topic)
                    reply(conn, ServerMessage(msg.topic, "ERROR", mapOf("reason" to "forbidden")))
                    return
                }
                topicRegistry.subscribe(msg.topic, conn)
                log.debugf("Connection %s subscribed to %s", conn.id(), msg.topic)
            }
            "UNSUBSCRIBE" -> {
                topicRegistry.unsubscribe(msg.topic, conn)
                log.debugf("Connection %s unsubscribed from %s", conn.id(), msg.topic)
            }
            else -> log.warnf("Unknown message type: %s", msg.type)
        }
    }

    @OnClose
    fun onClose(conn: WebSocketConnection) {
        topicRegistry.unsubscribeAll(conn)
        log.debugf("Connection closed: %s", conn.id())
    }

    private fun reply(conn: WebSocketConnection, message: ServerMessage) {
        conn.sendText(objectMapper.writeValueAsString(message))
            .subscribe().with({ }, { e -> log.warnf("Reply failed on %s: %s", conn.id(), e.message) })
    }
}
