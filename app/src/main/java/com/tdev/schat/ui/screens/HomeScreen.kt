package com.tdev.schat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tdev.schat.data.model.Chat
import com.tdev.schat.ui.theme.*
import com.tdev.schat.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenChat: (Chat) -> Unit
) {
    val chats by vm.chats.collectAsState()
    val currentUser by vm.currentUser.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SChat", style = MaterialTheme.typography.headlineMedium, color = Accent)
                        if (currentUser != null) {
                            Text(currentUser!!.username, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
                            if (currentUser!!.status.isNotBlank()) {
                                Text(
                                    text = "\"${currentUser!!.status}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Accent.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    // Durum butonu
                    IconButton(onClick = { showStatusDialog = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Durum", tint = OnSurfaceDim)
                    }
                    // Grup oluştur butonu
                    IconButton(onClick = { showGroupDialog = true }) {
                        Icon(Icons.Rounded.Group, contentDescription = "Grup oluştur", tint = OnSurfaceDim)
                    }
                }
            }

            HorizontalDivider(color = Outline, thickness = 0.5.dp)

            if (chats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Henüz kimse yok.", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceDim)
                        Spacer(Modifier.height(8.dp))
                        Text("Arkadaş eklemek için + kullan.", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim.copy(alpha = 0.6f))
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(chats, key = { it.id }) { chat ->
                        ChatRow(
                            chat = chat,
                            name = vm.chatDisplayName(chat),
                            myUid = currentUser?.uid ?: "",
                            isGroup = chat.isGroup,
                            onClick = { onOpenChat(chat) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = Outline, thickness = 0.5.dp)
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(24.dp).size(56.dp),
            containerColor = Accent,
            contentColor = Black,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Arkadaş ekle", modifier = Modifier.size(24.dp))
        }
    }

    if (showAddDialog) {
        AddFriendDialog(
            vm = vm,
            onDismiss = { showAddDialog = false; vm.clearAddFriendError() },
            onChatReady = { chat -> showAddDialog = false; onOpenChat(chat) }
        )
    }

    if (showGroupDialog) {
        CreateGroupDialog(
            vm = vm,
            onDismiss = { showGroupDialog = false; vm.clearGroupError() },
            onGroupReady = { chat -> showGroupDialog = false; onOpenChat(chat) }
        )
    }

    if (showStatusDialog) {
        StatusDialog(
            currentStatus = currentUser?.status ?: "",
            onDismiss = { showStatusDialog = false },
            onSave = { status -> vm.updateStatus(status); showStatusDialog = false }
        )
    }
}

@Composable
private fun ChatRow(
    chat: Chat,
    name: String,
    myUid: String,
    isGroup: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape).background(SurfaceVar),
            contentAlignment = Alignment.Center
        ) {
            if (isGroup) {
                Icon(Icons.Rounded.Group, contentDescription = null, tint = Accent, modifier = Modifier.size(24.dp))
            } else {
                Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = Accent, fontSize = 18.sp, style = MaterialTheme.typography.headlineMedium)
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.bodyLarge, color = OnSurface)
                if (chat.lastMessageTime > 0) {
                    Text(formatTime(chat.lastMessageTime), style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                }
            }
            Spacer(Modifier.height(3.dp))
            val prefix = if (chat.lastSenderId == myUid) "Sen: " else ""
            Text(
                text = if (chat.lastMessage.isNotEmpty()) "$prefix${chat.lastMessage}" else "Sohbete başla",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddFriendDialog(vm: MainViewModel, onDismiss: () -> Unit, onChatReady: (Chat) -> Unit) {
    var searchText by remember { mutableStateOf("") }
    val error by vm.addFriendError.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(24.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Arkadaş Ekle", style = MaterialTheme.typography.headlineMedium, color = OnSurface)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = OnSurfaceDim)
                    }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it; vm.clearAddFriendError() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("kullanıcı adı", color = OnSurfaceDim) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.addFriendAndOpenChat(searchText, onChatReady) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent, unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = Accent
                    ),
                    isError = error != null,
                    shape = RoundedCornerShape(10.dp)
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error!!, color = Color(0xFFFF5555), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { vm.addFriendAndOpenChat(searchText, onChatReady) },
                    enabled = searchText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black, disabledContainerColor = OutlineVar, disabledContentColor = OnSurfaceDim)
                ) { Text("Sohbet Başlat") }
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(vm: MainViewModel, onDismiss: () -> Unit, onGroupReady: (Chat) -> Unit) {
    var groupName by remember { mutableStateOf("") }
    var membersText by remember { mutableStateOf("") }
    val error by vm.groupError.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(24.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Grup Oluştur", style = MaterialTheme.typography.headlineMedium, color = OnSurface)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = OnSurfaceDim)
                    }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it; vm.clearGroupError() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Grup adı", color = OnSurfaceDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent, unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = Accent
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = membersText,
                    onValueChange = { membersText = it; vm.clearGroupError() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Üyeler (virgülle ayır: ali, veli)", color = OnSurfaceDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent, unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = Accent
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error!!, color = Color(0xFFFF5555), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val members = membersText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        vm.createGroup(groupName, members, onGroupReady)
                    },
                    enabled = groupName.isNotBlank() && membersText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black, disabledContainerColor = OutlineVar, disabledContentColor = OnSurfaceDim)
                ) { Text("Grubu Oluştur") }
            }
        }
    }
}

@Composable
private fun StatusDialog(currentStatus: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var statusText by remember { mutableStateOf(currentStatus) }

    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(24.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Durumunu Güncelle", style = MaterialTheme.typography.headlineMedium, color = OnSurface)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = OnSurfaceDim)
                    }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = statusText,
                    onValueChange = { statusText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ne düşünüyorsun?", color = OnSurfaceDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent, unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = Accent
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onSave(statusText) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)
                ) { Text("Kaydet") }
            }
        }
    }
}

private fun formatTime(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    return when {
        diff < 60_000 -> "şimdi"
        diff < 3_600_000 -> "${diff / 60_000}dk"
        diff < 86_400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(ts))
    }
}
