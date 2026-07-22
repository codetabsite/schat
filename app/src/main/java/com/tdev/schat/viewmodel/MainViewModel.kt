package com.tdev.schat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdev.schat.data.model.Chat
import com.tdev.schat.data.model.Message
import com.tdev.schat.data.model.User
import com.tdev.schat.data.repository.ChatRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    object NeedUsername : UiState()
    object Home : UiState()
}

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // Feature 2: Presence
    val otherIsOnline: Boolean = false,
    val otherLastSeen: Long = 0L,
    // Feature 5: Typing
    val typingUsers: Map<String, Boolean> = emptyMap()
)

class MainViewModel : ViewModel() {

    val repo = ChatRepository()

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val _addFriendError = MutableStateFlow<String?>(null)
    val addFriendError: StateFlow<String?> = _addFriendError.asStateFlow()

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    private val _groupError = MutableStateFlow<String?>(null)
    val groupError: StateFlow<String?> = _groupError.asStateFlow()

    // Unread counts per chatId
    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts.asStateFlow()

    // Typing debounce job
    private var typingJob: Job? = null
    private var currentChatId: String? = null

    init { checkAuth() }

    private fun checkAuth() {
        viewModelScope.launch {
            if (repo.isLoggedIn) {
                val uid = repo.currentUid ?: return@launch
                val user = repo.getUser(uid)
                if (user == null) {
                    _uiState.value = UiState.NeedUsername
                } else {
                    _currentUser.value = user
                    _uiState.value = UiState.Home
                    observeChats(uid)
                    refreshFcmToken(uid)
                    // Feature 2: Setup presence
                    repo.setupPresence(uid)
                }
            } else {
                _uiState.value = UiState.NeedUsername
            }
        }
    }

    private fun refreshFcmToken(uid: String) {
        viewModelScope.launch {
            val token = repo.getFcmToken() ?: return@launch
            repo.saveFcmToken(uid, token)
        }
    }

    fun setupUser(username: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            try {
                val uid = if (repo.isLoggedIn) repo.currentUid!! else repo.signInAnonymously()
                val existing = repo.findUserByUsername(username.trim())
                if (existing != null && existing.uid != uid) {
                    _addFriendError.value = "Bu kullanıcı adı alınmış."
                    return@launch
                }
                repo.createUser(uid, username.trim())
                val user = repo.getUser(uid) ?: return@launch
                _currentUser.value = user
                _uiState.value = UiState.Home
                observeChats(uid)
                refreshFcmToken(uid)
                repo.setupPresence(uid)
            } catch (e: Exception) {
                _addFriendError.value = e.message
            }
        }
    }

    private fun observeChats(uid: String) {
        viewModelScope.launch {
            repo.observeUserChats(uid).collect { chatList ->
                _chats.value = chatList
                // Observe unread count for each chat
                chatList.forEach { chat ->
                    launch {
                        repo.getUnreadCount(chat.id, uid).collect { count ->
                            _unreadCounts.value = _unreadCounts.value + (chat.id to count)
                        }
                    }
                }
            }
        }
    }

    fun addFriendAndOpenChat(username: String, onChatReady: (Chat) -> Unit) {
        val me = _currentUser.value ?: return
        if (username.trim().equals(me.username, ignoreCase = true)) {
            _addFriendError.value = "Kendinle sohbet edemezsin."
            return
        }
        viewModelScope.launch {
            try {
                val other = repo.findUserByUsername(username.trim())
                if (other == null) {
                    _addFriendError.value = "Kullanıcı bulunamadı: $username"
                    return@launch
                }
                val chat = repo.getOrCreateChat(me.uid, me.username, other)
                _addFriendError.value = null
                onChatReady(chat)
            } catch (e: Exception) {
                _addFriendError.value = e.message
            }
        }
    }

    fun createGroup(groupName: String, memberUsernames: List<String>, onDone: (Chat) -> Unit) {
        val me = _currentUser.value ?: return
        if (groupName.isBlank()) { _groupError.value = "Grup adı boş olamaz."; return }
        if (memberUsernames.isEmpty()) { _groupError.value = "En az bir üye ekle."; return }
        viewModelScope.launch {
            try {
                val chat = repo.createGroup(me.uid, me.username, groupName.trim(), memberUsernames)
                _groupError.value = null
                onDone(chat)
            } catch (e: Exception) {
                _groupError.value = e.message
            }
        }
    }

    fun updateStatus(status: String) {
        val uid = _currentUser.value?.uid ?: return
        viewModelScope.launch {
            try {
                repo.updateStatus(uid, status.trim())
                _currentUser.value = _currentUser.value?.copy(status = status.trim())
            } catch (_: Exception) {}
        }
    }

    fun clearAddFriendError() { _addFriendError.value = null }
    fun clearGroupError() { _groupError.value = null }

    fun observeMessages(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            repo.observeMessages(chatId).collect { msgs ->
                _chatState.value = _chatState.value.copy(messages = msgs, isLoading = false)
                // Feature 1: Auto-mark messages as read when chat is open
                val myUid = _currentUser.value?.uid ?: return@collect
                repo.markMessagesAsRead(chatId, myUid)
            }
        }
        // Feature 5: Observe typing indicators
        viewModelScope.launch {
            repo.observeTypingUsers(chatId).collect { typingMap ->
                _chatState.value = _chatState.value.copy(typingUsers = typingMap)
            }
        }
    }

    // Feature 2: Observe presence of the other user
    fun observePresence(otherUid: String) {
        viewModelScope.launch {
            repo.observeUserPresence(otherUid).collect { (isOnline, lastSeen) ->
                _chatState.value = _chatState.value.copy(
                    otherIsOnline = isOnline,
                    otherLastSeen = lastSeen
                )
            }
        }
    }

    fun sendText(chatId: String, text: String) {
        val me = _currentUser.value ?: return
        if (text.isBlank()) return
        // Stop typing indicator on send
        notifyTyping(chatId, false)
        viewModelScope.launch {
            try {
                repo.sendTextMessage(chatId, me.uid, me.username, text.trim())
            } catch (e: Exception) {
                _sendError.value = e.message
            }
        }
    }

    // Feature 5: Typing indicator with auto-stop after 3s
    fun notifyTyping(chatId: String, isTyping: Boolean) {
        val uid = _currentUser.value?.uid ?: return
        typingJob?.cancel()
        repo.setTyping(chatId, uid, isTyping)
        if (isTyping) {
            typingJob = viewModelScope.launch {
                delay(3000)
                repo.setTyping(chatId, uid, false)
            }
        }
    }

    // Feature 8: Toggle emoji reaction
    fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        val uid = _currentUser.value?.uid ?: return
        repo.toggleReaction(chatId, messageId, uid, emoji)
    }

    fun clearSendError() { _sendError.value = null }

    fun chatDisplayName(chat: Chat): String {
        if (chat.isGroup) return chat.groupName
        val myUid = currentUser.value?.uid ?: return ""
        return chat.participants.entries.firstOrNull { it.key != myUid }?.value ?: "?"
    }

    fun otherParticipantName(chat: Chat) = chatDisplayName(chat)

    fun getOtherUid(chat: Chat): String? {
        val myUid = currentUser.value?.uid ?: return null
        return chat.participants.keys.firstOrNull { it != myUid }
    }
}
