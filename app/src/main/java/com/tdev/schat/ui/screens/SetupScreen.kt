package com.tdev.schat.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tdev.schat.ui.theme.*
import com.tdev.schat.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun SetupScreen(vm: MainViewModel) {
    var username by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val error by vm.addFriendError.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
        delay(400)
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Logo mark
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        color = Black,
                        fontSize = 28.sp,
                        style = MaterialTheme.typography.headlineLarge
                    )
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "Hoş geldin.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Bir kullanıcı adı seç.\nArkadaşların seni bununla bulacak.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceDim,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(40.dp))

                // Input
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        if (it.length <= 24) username = it
                        vm.clearAddFriendError()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text("kullanıcı adı", color = OnSurfaceDim)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (username.isNotBlank()) vm.setupUser(username) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = Accent,
                        errorBorderColor = Color(0xFFFF5555),
                    ),
                    isError = error != null,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        color = Color(0xFFFF5555),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { vm.setupUser(username) },
                    enabled = username.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Black,
                        disabledContainerColor = OutlineVar,
                        disabledContentColor = OnSurfaceDim
                    )
                ) {
                    Text(
                        text = "Devam",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
