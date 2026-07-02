package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SparkViewModel
import com.example.ui.theme.*

@Composable
fun SettingsTab(
    viewModel: SparkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var feedbackEmail by remember { mutableStateOf("") }
    var feedbackRating by remember { mutableStateOf(5) }
    var feedbackComment by remember { mutableStateOf("") }
    var feedbackSentSuccess by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App settings header
        item {
            Text(
                text = "Spark Ayarlar & Sistem",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Spotify entegrasyonu ve bağlantı ayarları.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Spotify credentials information card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, SpotifyGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SpotifyGreenBright,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Spotify Developer Entegrasyonu",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Bu uygulama gerçek Spotify API bağlantıları kullanır. Spotify Client ID ve Secret değerleriniz .env üzerinden güvenle saklanmaktadır. Eğer bağlantı sağlanamazsa, uygulama otomatik olarak yerel müzikal sentez motorunu devreye sokar.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Offline Mode Simulator Card
        item {
            val isOfflineSimulated by viewModel.isOfflineModeSimulated.collectAsState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicSurface, RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (isOfflineSimulated) Color.Red.copy(alpha = 0.4f) else GlassBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = if (isOfflineSimulated) Color.Red else SpotifyGreenBright,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Çevrimdışı Mod Simülatörü",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Offline-First senkronizasyonunu test et.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Switch(
                        checked = isOfflineSimulated,
                        onCheckedChange = { viewModel.setOfflineModeSimulated(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Red,
                            checkedTrackColor = Color.Red.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextDisabled,
                            uncheckedTrackColor = CosmicSurfaceElevated
                        ),
                        modifier = Modifier.testTag("offline_simulator_switch")
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Bu seçeneği açarak internet bağlantısının koptuğu durumları simüle edebilirsiniz. Gönderdiğiniz mesajlar önce 'Hata' durumuna düşer. Tekrar kapatıp 'Yeniden Gönder' ikonuna tıkladığınızda anında eşitlenir!",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Interactive Feedback reporting card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RateReview,
                        contentDescription = null,
                        tint = SparkAccentPink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Geri Bildirim Gönderin",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (feedbackSentSuccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpotifyGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Harika! Geri bildiriminiz başarıyla sisteme kaydedildi ve admin paneline iletildi. Teşekkür ederiz! ❤️🎸",
                            color = SpotifyGreenBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { feedbackSentSuccess = false }) {
                        Text("Yeni Geri Bildirim Yaz", color = SpotifyGreenBright, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedTextField(
                        value = feedbackEmail,
                        onValueChange = { feedbackEmail = it },
                        label = { Text("E-posta Adresiniz", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpotifyGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_email_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Uygulama Deneyim Puanınız:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(5) { index ->
                            val score = index + 1
                            Text(
                                text = "⭐",
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clickable { feedbackRating = score }
                                    .padding(4.dp),
                                color = if (score <= feedbackRating) Color.Yellow else TextDisabled
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = feedbackComment,
                        onValueChange = { feedbackComment = it },
                        label = { Text("Yorum ve Önerileriniz", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpotifyGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_comment_input"),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (feedbackEmail.isNotBlank() && feedbackComment.isNotBlank()) {
                                viewModel.sendUserFeedback(
                                    feedbackEmail,
                                    feedbackRating,
                                    feedbackComment,
                                    onSuccess = {
                                        feedbackSentSuccess = true
                                        feedbackEmail = ""
                                        feedbackComment = ""
                                        Toast.makeText(context, "Geri bildiriminiz iletildi!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_feedback_button")
                    ) {
                        Text(
                            text = "Geri Bildirimi Gönder",
                            color = CosmicBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // About the version and details
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Spark Premium v1.0.4",
                    color = TextDisabled,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Made with ❤️ in Kadıköy",
                    color = TextDisabled,
                    fontSize = 11.sp
                )
            }
        }
    }
}
