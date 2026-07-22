package com.tdev.schat.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tdev.schat.data.model.Chat
import com.tdev.schat.data.model.Message
import com.tdev.schat.ui.theme.*
import com.tdev.schat.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// Available emoji reactions
private val REACTION_EMOJIS = listOf("❤️", "😂", "👍", "😮", "😢", "🔥")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chat: Chat,
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val chatState by vm.chatState.collectAsState()
    val currentUser by vm.currentUser.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val otherName = vm.otherParticipantName(chat)

    // Typing indicator: other users who are typing (excluding me)
    val myUid = currentUser?.uid ?: ""
    val othersTyping = chatState.typingUsers
        .filter { (uid, isTyping) -> uid != myUid && isTyping }
        .isNotEmpty()

    // Participant names for typing label
    val typingNames = chatState.typingUsers
        .filter { (uid, isTyping) -> uid != myUid && isTyping }
        .keys
        .mapNotNull { uid -> chat.participants[uid] }

    LaunchedEffect(chat.id) {
        vm.observeMessages(chat.id)
        // Feature 2: Start observing presence of other user (1-on-1 only)
        if (!chat.isGroup) {
            val otherUid = vm.getOtherUid(chat)
            if (otherUid != null) vm.observePresence(otherUid)
        }
    }

    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Geri",
                        tint = OnSurface
                    )
                }
                // Avatar with online dot
                Box(modifier = Modifier.size(40.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceVar)
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = otherName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color = Accent,
                            fontSize = 15.sp
                        )
                    }
                    // Feature 2: Online dot
                    if (!chat.isGroup && chatState.otherIsOnline) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(OnlineGreen)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = otherName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurface
                    )
                    // Feature 2: Online / last seen
                    if (!chat.isGroup) {
                        Text(
                            text = if (chatState.otherIsOnline) "çevrimiçi"
                                   else if (chatState.otherLastSeen > 0) "son görülme: ${formatLastSeen(chatState.otherLastSeen)}"
                                   else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (chatState.otherIsOnline) OnlineGreen else OnSurfaceDim
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Outline, thickness = 0.5.dp)

        // Messages
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chatState.messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == currentUser?.uid
                    MessageBubble(
                        msg = msg,
                        isMe = isMe,
                        showSender = chat.isGroup && !isMe,
                        onLongPress = { /* handled inside */ },
                        onReaction = { emoji -> vm.toggleReaction(chat.id, msg.id, emoji) }
                    )
                }
            }

            if (chatState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = Accent,
                    trackColor = Outline
                )
            }
        }

        // Feature 5: Typing indicator
        AnimatedVisibility(
            visible = othersTyping,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val label = when {
                typingNames.isEmpty() -> "yazıyor..."
                typingNames.size == 1 -> "${typingNames[0]} yazıyor..."
                else -> "${typingNames.joinToString(", ")} yazıyor..."
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
            }
        }

        // Input bar
        HorizontalDivider(color = Outline, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    // Feature 5: Notify typing
                    if (it.isNotBlank()) vm.notifyTyping(chat.id, true)
                    else vm.notifyTyping(chat.id, false)
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mesaj", color = OnSurfaceDim) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Outline,
                    unfocusedBorderColor = Outline,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    cursorColor = Accent,
                    focusedContainerColor = SurfaceVar,
                    unfocusedContainerColor = SurfaceVar
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 5,
                textStyle = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) Accent else OutlineVar),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        vm.sendText(chat.id, inputText)
                        inputText = ""
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Gönder",
                        tint = if (inputText.isNotBlank()) Black else OnSurfaceDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    msg: Message,
    isMe: Boolean,
    showSender: Boolean = false,
    onLongPress: () -> Unit,
    onReaction: (String) -> Unit
) {
    val bubbleColor = if (isMe) BubbleMe else BubbleThem
    val align = if (isMe) Alignment.End else Alignment.Start
    val shape = if (isMe) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    var showReactionPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { showReactionPicker = true })
                }
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (showSender) {
                    Text(
                        text = msg.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurface
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatMsgTime(msg.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceDim
                    )
                    // Feature 1: Read receipt tick
                    if (isMe) {
                        Text(
                            text = if (msg.isRead) "✓✓" else "✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (msg.isRead) Accent else OnSurfaceDim,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Feature 8: Show existing reactions
        if (msg.reactions.isNotEmpty()) {
            val grouped = msg.reactions.values.groupBy { it }
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (emoji, users) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceVar,
                        modifier = Modifier
                    ) {
                        Text(
                            text = if (users.size > 1) "$emoji ${users.size}" else emoji,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Feature 8: Reaction picker (shown on long press)
        if (showReactionPicker) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceVar,
                shadowElevation = 4.dp,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    REACTION_EMOJIS.forEach { emoji ->
                        TextButton(
                            onClick = {
                                onReaction(emoji)
                                showReactionPicker = false
                            },
                            contentPadding = PaddingValues(4.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(emoji, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatMsgTime(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

private fun formatLastSeen(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    return when {
        diff < 60_000 -> "az önce"
        diff < 3_600_000 -> "${diff / 60_000} dk önce"
        diff < 86_400_000 -> "${diff / 3_600_000} sa önce"
        else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(ts))
    }
}
