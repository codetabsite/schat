package com.tdev.schat.data.model

data class User(
    val uid: String = "",
    val username: String = "",
    val status: String = "",
    val fcmToken: String = "",
    val oneSignalId: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    // Feature 1: Read receipts
    val isRead: Boolean = false,
    // Feature 8: Emoji reactions — map of userId -> emoji
    val reactions: Map<String, String> = emptyMap()
)

data class Chat(
    val id: String = "",
    val participants: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastSenderId: String = "",
    val isGroup: Boolean = false,
    val groupName: String = "",
    // Feature 5: Typing indicators — set of userIds currently typing
    val typingUsers: Map<String, Boolean> = emptyMap()
)
