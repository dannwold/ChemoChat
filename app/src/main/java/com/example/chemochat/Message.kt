package com.example.chemochat

import android.graphics.Bitmap
import android.net.Uri

/**
 * Data class representing a chat message.
 */
data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val content: String, // Encrypted Base64 string
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = true,
    val localUri: Uri? = null, // For images/audio stored locally
    val decryptedContent: String? = null // Cached decrypted text
)

enum class MessageType {
    TEXT, IMAGE, AUDIO
}
