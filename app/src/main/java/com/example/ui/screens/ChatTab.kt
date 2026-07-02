package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.Match
import com.example.data.SyncStatus
import com.example.ui.SparkViewModel
import com.example.ui.theme.*

@Composable
fun ChatTab(
    viewModel: SparkViewModel,
    modifier: Modifier = Modifier
) {
    val matches by viewModel.matches.collectAsState()
    var activeMatch by remember { mutableStateOf<Match?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
    ) {
        if (activeMatch != null) {
            // Live Chat Screen
            LiveChatView(
                match = activeMatch!!,
                viewModel = viewModel,
                onBack = { activeMatch = null }
            )
        } else {
            // Match List Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Müzikal Eşleşmelerin",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (matches.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = TextDisabled,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Henüz eşleşme kurulmadı.",
                            color = TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ruh ikizini bulmak için Keşfet sekmesinde şarkıları dinlemeye başla!",
                            color = TextDisabled,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(matches) { match ->
                            MatchItemRow(match = match, onClick = { activeMatch = match })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchItemRow(
    match: Match,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmicSurface, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
            .testTag("match_item_row"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(SpotifyGreen.copy(alpha = 0.2f), CircleShape)
                .border(2.dp, SpotifyGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = match.userName.take(1).uppercase(),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = match.userName,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = match.lastMessage,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LiveChatView(
    match: Match,
    viewModel: SparkViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.getMessagesForMatch(match.id).collectAsState(initial = emptyList())
    var textMessage by remember { mutableStateOf("") }
    val isOfflineSimulated by viewModel.isOfflineModeSimulated.collectAsState()
    val hasFailedMessage = remember(messages) { messages.any { it.syncStatus == SyncStatus.FAILED } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CosmicSurface)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SpotifyGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = match.userName.take(1).uppercase(),
                    color = CosmicBackground,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = match.userName,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (isOfflineSimulated) {
                    Text(
                        text = "🔴 Çevrimdışı (Simülasyon)",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "⚡ Supabase Realtime Aktif",
                        color = SpotifyGreenBright,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (hasFailedMessage) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C1E21))
                    .border(1.dp, Color.Red.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Hata",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bağlantı sorunu! Bazı mesajlar gönderilemedi.",
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Historic message lines
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                val isMe = message.senderId == "me"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMe && message.syncStatus == SyncStatus.FAILED) {
                        IconButton(
                            onClick = { viewModel.retryChatMessage(message.id) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("retry_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Yeniden Gönder",
                                tint = Color.Red,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .background(
                                color = if (isMe) SpotifyGreen else CosmicSurfaceElevated,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                )
                            )
                            .border(
                                1.dp,
                                if (isMe) {
                                    if (message.syncStatus == SyncStatus.FAILED) Color.Red.copy(alpha = 0.5f)
                                    else SpotifyGreenBright.copy(alpha = 0.3f)
                                } else GlassBorder,
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = message.text,
                                color = if (isMe) CosmicBackground else TextPrimary,
                                fontSize = 14.sp
                            )
                            
                            if (isMe) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .padding(top = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    when (message.syncStatus) {
                                        SyncStatus.PENDING_INSERT -> {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = "Gönderiliyor",
                                                tint = CosmicBackground.copy(alpha = 0.6f),
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                        SyncStatus.SYNCED -> {
                                            Icon(
                                                imageVector = Icons.Default.DoneAll,
                                                contentDescription = "Eşitlendi",
                                                tint = CosmicBackground.copy(alpha = 0.8f),
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                        SyncStatus.FAILED -> {
                                            Text(
                                                text = "Hata",
                                                color = Color.Red,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Message input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CosmicSurface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textMessage,
                onValueChange = { textMessage = it },
                placeholder = { Text("Mesajınızı yazın...", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textMessage.isNotBlank()) {
                        viewModel.sendChatMessage(match.id, textMessage)
                        textMessage = ""
                    }
                },
                modifier = Modifier
                    .background(SpotifyGreen, CircleShape)
                    .size(48.dp)
                    .testTag("send_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = CosmicBackground
                )
            }
        }
    }
}
