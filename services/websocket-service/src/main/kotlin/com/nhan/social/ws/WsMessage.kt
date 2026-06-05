package com.nhan.social.ws

data class ClientMessage(
    val type: String,   // "SUBSCRIBE" | "UNSUBSCRIBE"
    val topic: String,
)

data class ServerMessage(
    val topic: String,
    val type: String,
    val payload: Map<String, Any?>,
)
