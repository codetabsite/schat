package com.tdev.schat.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.tdev.schat.data.model.Chat
import com.tdev.schat.data.model.Message
import com.tdev.schat.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    val currentUid: String?
        get() = auth.currentUser?.uid

    // ── Auth ─────────────────────────────────────────────────────────────────

    suspend fun signInAnonymously(): String {
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("Anonymous sign-in failed")
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    suspend fun getUser(uid: String): User? {
        val snap = db.child("users").child(uid).get().await()
        return snap.getValue(User::class.java)
    }

    suspend fun createUser(uid: String, username: String) {
        val user = User(uid = uid, username = username)
        db.child("users").child(uid).setValue(user).await()
    }

    suspend fun findUserByUsername(username: String): User? {
        val snap = db.child("users")
            .orderByChild("username")
            .equalTo(username)
            .get()
            .await()
        return snap.children.firstOrNull()?.getValue(User::class.java)
    }

    suspend fun updateStatus(uid: String, status: String) {
        db.child("users").child(uid).child("status").setValue(status).await()
    }

    // ── Feature 2: Online Presence ────────────────────────────────────────────

    fun setupPresence(uid: String) {
        val userOnlineRef = db.child("users").child(uid).child("isOnline")
        val userLastSeenRef = db.child("users").child(uid).child("lastSeen")
        val connectedRef = db.child(".info/connected")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    userOnlineRef.setValue(true)
                    // When disconnected, set offline + lastSeen
                    userOnlineRef.onDisconnect().setValue(false)
                    userLastSeenRef.onDisconnect().setValue(ServerValue.TIMESTAMP)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun observeUserPresence(uid: String): Flow<Pair<Boolean, Long>> = callbackFlow {
        val ref = db.child("users").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                trySend(Pair(isOnline, lastSeen))
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── FCM ───────────────────────────────────────────────────────────────────

    suspend fun getFcmToken(): String? =
        runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()

    suspend fun saveFcmToken(uid: String, token: String) {
        db.child("users").child(uid).child("fcmToken").setValue(token).await()
    }

    // ── Chats ─────────────────────────────────────────────────────────────────

    suspend fun getOrCreateChat(myUid: String, myUsername: String, other: User): Chat {
        val chatId = if (myUid < other.uid) "${myUid}_${other.uid}" else "${other.uid}_${myUid}"
        val ref = db.child("chats").child(chatId)

        val existing = ref.get().await().getValue(Chat::class.java)
        if (existing != null) return existing

        val chat = Chat(
            id = chatId,
            participants = mapOf(myUid to myUsername, other.uid to other.username),
            isGroup = false
        )
        ref.setValue(chat).await()

        db.child("userChats").child(myUid).child(chatId).setValue(true).await()
        db.child("userChats").child(other.uid).child(chatId).setValue(true).await()

        return chat
    }

    suspend fun createGroup(
        myUid: String,
        myUsername: String,
        groupName: String,
        memberUsernames: List<String>
    ): Chat {
        val chatRef = db.child("chats").push()
        val chatId = chatRef.key ?: error("Could not generate chat id")

        val participants = mutableMapOf(myUid to myUsername)
        for (uname in memberUsernames) {
            val user = findUserByUsername(uname)
                ?: throw IllegalArgumentException("Kullanıcı bulunamadı: $uname")
            participants[user.uid] = user.username
        }

        val chat = Chat(
            id = chatId,
            participants = participants,
            isGroup = true,
            groupName = groupName
        )
        chatRef.setValue(chat).await()

        for (uid in participants.keys) {
            db.child("userChats").child(uid).child(chatId).setValue(true).await()
        }

        return chat
    }

    fun observeUserChats(uid: String): Flow<List<Chat>> = callbackFlow {
        val userChatsRef = db.child("userChats").child(uid)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatIds = snapshot.children.map { it.key ?: "" }.filter { it.isNotEmpty() }
                if (chatIds.isEmpty()) { trySend(emptyList()); return }

                val chats = mutableListOf<Chat>()
                var remaining = chatIds.size

                for (chatId in chatIds) {
                    db.child("chats").child(chatId).get()
                        .addOnSuccessListener { snap ->
                            snap.getValue(Chat::class.java)?.let { chats.add(it) }
                            if (--remaining == 0) trySend(chats.sortedByDescending { it.lastMessageTime })
                        }
                        .addOnFailureListener {
                            if (--remaining == 0) trySend(chats.sortedByDescending { it.lastMessageTime })
                        }
                }
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }

        userChatsRef.addValueEventListener(listener)
        awaitClose { userChatsRef.removeEventListener(listener) }
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val ref = db.child("messages").child(chatId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msgs = snapshot.children
                    .mapNotNull { it.getValue(Message::class.java) }
                    .sortedBy { it.timestamp }
                trySend(msgs)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun sendTextMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String
    ) {
        val msgRef = db.child("messages").child(chatId).push()
        val msgId = msgRef.key ?: error("Could not generate message id")
        val now = System.currentTimeMillis()

        val message = Message(
            id = msgId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = now,
            isRead = false
        )
        msgRef.setValue(message).await()

        db.child("chats").child(chatId).updateChildren(
            mapOf(
                "lastMessage" to text,
                "lastMessageTime" to now,
                "lastSenderId" to senderId
            )
        ).await()
    }

    // ── Feature 1: Read Receipts ──────────────────────────────────────────────

    fun markMessagesAsRead(chatId: String, currentUserId: String) {
        db.child("messages").child(chatId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { child ->
                        val senderId = child.child("senderId").getValue(String::class.java)
                        val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                        // Mark as read only messages from the OTHER person
                        if (senderId != null && senderId != currentUserId && !isRead) {
                            child.ref.child("isRead").setValue(true)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun getUnreadCount(chatId: String, currentUserId: String): Flow<Int> = callbackFlow {
        val ref = db.child("messages").child(chatId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.children.count { child ->
                    val senderId = child.child("senderId").getValue(String::class.java)
                    val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                    senderId != null && senderId != currentUserId && !isRead
                }
                trySend(count)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Feature 5: Typing Indicator ───────────────────────────────────────────

    fun setTyping(chatId: String, uid: String, isTyping: Boolean) {
        db.child("chats").child(chatId).child("typingUsers").child(uid).setValue(isTyping)
    }

    fun observeTypingUsers(chatId: String): Flow<Map<String, Boolean>> = callbackFlow {
        val ref = db.child("chats").child(chatId).child("typingUsers")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val typingMap = mutableMapOf<String, Boolean>()
                snapshot.children.forEach { child ->
                    val uid = child.key ?: return@forEach
                    val typing = child.getValue(Boolean::class.java) ?: false
                    typingMap[uid] = typing
                }
                trySend(typingMap)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Feature 8: Emoji Reactions ────────────────────────────────────────────

    fun toggleReaction(chatId: String, messageId: String, userId: String, emoji: String) {
        val reactionRef = db.child("messages").child(chatId).child(messageId)
            .child("reactions").child(userId)

        reactionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val current = snapshot.getValue(String::class.java)
                if (current == emoji) {
                    // Same emoji → remove (toggle off)
                    reactionRef.removeValue()
                } else {
                    // New or different emoji → set
                    reactionRef.setValue(emoji)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
