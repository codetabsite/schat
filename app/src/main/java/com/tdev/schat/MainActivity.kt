package com.tdev.schat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tdev.schat.data.model.Chat
import com.tdev.schat.ui.screens.ChatScreen
import com.tdev.schat.ui.screens.HomeScreen
import com.tdev.schat.ui.screens.SetupScreen
import com.tdev.schat.ui.theme.Accent
import com.tdev.schat.ui.theme.Black
import com.tdev.schat.ui.theme.SChatTheme
import com.tdev.schat.viewmodel.MainViewModel
import com.tdev.schat.viewmodel.UiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SChatTheme {
                SChatApp()
            }
        }
    }
}

@Composable
fun SChatApp() {
    val vm: MainViewModel = viewModel()
    val uiState by vm.uiState.collectAsState()
    val navController = rememberNavController()

    // Active chat stored in memory for nav
    var activeChat by remember { mutableStateOf<Chat?>(null) }

    when (uiState) {
        UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent, strokeWidth = androidx.compose.ui.unit.Dp(2f))
            }
        }
        UiState.NeedUsername -> SetupScreen(vm = vm)
        UiState.Home -> {
            NavHost(
                navController = navController,
                startDestination = "home",
                enterTransition  = { slideInHorizontally { it } + fadeIn() },
                exitTransition   = { slideOutHorizontally { -it / 3 } + fadeOut() },
                popEnterTransition  = { slideInHorizontally { -it } + fadeIn() },
                popExitTransition   = { slideOutHorizontally { it } + fadeOut() }
            ) {
                composable("home") {
                    HomeScreen(
                        vm = vm,
                        onOpenChat = { chat ->
                            activeChat = chat
                            navController.navigate("chat")
                        }
                    )
                }
                composable("chat") {
                    activeChat?.let { chat ->
                        ChatScreen(
                            chat = chat,
                            vm = vm,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
