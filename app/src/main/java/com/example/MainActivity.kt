package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AuthUiState
import com.example.ui.SparkViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.CosmicBackground
import com.example.ui.theme.CosmicSurface

class MainActivity : ComponentActivity() {
    private val viewModel: SparkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle deep link on cold start
        intent?.let { handleDeepLink(it) }

        setContent {
            MyApplicationTheme {
                val authState by viewModel.authState.collectAsState()

                when (authState) {
                    AuthUiState.Loading -> SplashScreen()
                    AuthUiState.LoggedOut -> LoginScreen(authService = viewModel.authService)
                    AuthUiState.LoggedIn -> MainScaffold(viewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopAudio()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopAudio()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null && data.scheme == "spark" && data.host == "login") {
            viewModel.handleAuthDeeplink(intent)
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Spark",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = SpotifyGreen)
        }
    }
}

@Composable
private fun MainScaffold(viewModel: SparkViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Keşfet, 1 = Sohbet, 2 = Profil, 3 = Ayarlar

    Scaffold(
        modifier = Modifier.fillMaxSize().background(CosmicBackground),
        bottomBar = {
            NavigationBar(
                containerColor = CosmicSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SpotifyGreen,
                    selectedTextColor = SpotifyGreen,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = CosmicSurface
                )
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; viewModel.stopAudio() },
                    label = { Text("Keşfet", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = "Keşfet") },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; viewModel.stopAudio() },
                    label = { Text("Sohbet", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Sohbet") },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; viewModel.stopAudio() },
                    label = { Text("Profil", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3; viewModel.stopAudio() },
                    label = { Text("Ayarlar", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ayarlar") },
                    colors = itemColors
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DiscoverTab(viewModel = viewModel)
                1 -> ChatTab(viewModel = viewModel)
                2 -> ProfileTab(viewModel = viewModel)
                3 -> SettingsTab(viewModel = viewModel)
            }
        }
    }
}
