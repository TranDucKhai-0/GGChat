package com.example.ggchat

data class Message(
    /**
     * Sender identifier.
     * - For clients: the server assigns this based on the TCP connection (e.g., "ip:remotePort").
     * - For the host: use "HOST".
     */
    val senderId: String,
    /** Display name (normalized on the server) so older messages keep context. */
    val senderName: String,
    /** Avatar as Base64 (may be included in CHAT_BROADCAST for backward compatibility). */
    val senderAvatarBase64: String?,
    val text: String,
    val time: Long
)
