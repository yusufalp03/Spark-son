package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SparkViewModel
import com.example.ui.theme.*

@Composable
fun AdminPanel(
    viewModel: SparkViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.systemLogs.collectAsState()
    val feedbacks by viewModel.feedbackList.collectAsState()
    val profiles by viewModel.discoverProfiles.collectAsState()
    val matches by viewModel.matches.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Logs, 1 = Feedbacks

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .padding(24.dp)
    ) {
        Text(
            text = "Developer Admin Portalı",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Spark sistem metrikleri ve gerçek zamanlı denetleme paneli.",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // System Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Eşleşme Oranı",
                value = "${matches.size} Eşleşme",
                icon = Icons.Default.Speed,
                color = SpotifyGreen,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Kuyruktaki Profiller",
                value = "${profiles.size} Profil",
                icon = Icons.Default.BugReport,
                color = SparkAccentPink,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tab Selector (Logs vs Feedbacks)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CosmicSurface,
            contentColor = SpotifyGreen,
            modifier = Modifier
                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Canlı Performans Logları (${logs.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Geri Bildirimler (${feedbacks.size})", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                // System Logs view
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Canlı Sistem Olayları",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { viewModel.clearSystemLogs() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Logları Temizle", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (logs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Sistem log kaydı bulunamadı.", color = TextDisabled)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(logs) { log ->
                                LogRow(log = log)
                            }
                        }
                    }
                }
            } else {
                // Feedback logs
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Kullanıcı Geri Bildirimleri",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    if (feedbacks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Geri bildirim kaydı bulunamadı.", color = TextDisabled)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(feedbacks) { fb ->
                                FeedbackRow(feedback = fb)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CosmicSurface, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = title, color = TextSecondary, fontSize = 12.sp)
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LogRow(log: com.example.data.SystemLog) {
    val levelColor = when (log.level.uppercase()) {
        "ERROR" -> Color.Red
        "WARNING" -> Color(0xFFFF9800)
        else -> SpotifyGreenBright
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmicSurface, RoundedCornerShape(10.dp))
            .border(1.dp, levelColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "[${log.level}] - ${log.tag}",
                color = levelColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                text = "Perf: OK",
                color = TextDisabled,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = log.message,
            color = TextPrimary,
            fontSize = 12.sp
        )
    }
}

@Composable
fun FeedbackRow(feedback: com.example.data.UserProfileDao /* Wait, we should import UserFeedback from data models. It's fully visible inside com.example.data */) {
    // Actually our feedback type is com.example.data.UserFeedback.
}

@Composable
fun FeedbackRow(feedback: com.example.data.UserFeedback) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmicSurface, RoundedCornerShape(10.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = feedback.email,
                color = SpotifyGreenBright,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Row {
                repeat(feedback.rating) {
                    Text("⭐", fontSize = 10.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = feedback.comment,
            color = TextPrimary,
            fontSize = 13.sp
        )
    }
}
