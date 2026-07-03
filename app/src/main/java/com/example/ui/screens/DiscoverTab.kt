package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DiscoverProfile
import com.example.ui.SparkViewModel
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DiscoverTab(
    viewModel: SparkViewModel,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.discoverProfiles.collectAsState()
    val isRefreshing by viewModel.isDiscoverRefreshing.collectAsState()
    val activePlayingId by viewModel.activePlayingProfileId.collectAsState()
    val isPlaying by viewModel.isAudioPlaying.collectAsState()
    val celebrationProfile by viewModel.matchCelebrationProfile.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
    ) {
        if (isRefreshing && profiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SpotifyGreen)
            }
        } else if (profiles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(64.dp)
                        .clickable { viewModel.refreshDiscoverQueue() }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Etrafta yeni ruh ikizleri aranıyor...",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Müzikal uyum listenizi yenilemek için tıklayın.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.refreshDiscoverQueue() },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Yenile", color = CosmicBackground, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Tinder-like Stack UI
            val topProfile = profiles.first()
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                DiscoverCard(
                    profile = topProfile,
                    isPlaying = isPlaying && activePlayingId == topProfile.id,
                    onPlayToggle = { viewModel.playAudioForProfile(topProfile) },
                    onLike = { viewModel.swipeRight(topProfile.id) },
                    onPass = { viewModel.swipeLeft(topProfile.id) }
                )
            }
        }

        // Background loading progress bar
        if (isRefreshing && profiles.isNotEmpty()) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .height(4.dp),
                color = SpotifyGreen,
                trackColor = Color.Transparent
            )
        }

        // Real-time Match celebration dialogue
        celebrationProfile?.let { matchedUser ->
            MatchCelebrationDialog(
                profile = matchedUser,
                onDismiss = { viewModel.dismissMatchCelebration() }
            )
        }
    }
}

@Composable
fun DiscoverCard(
    profile: DiscoverProfile,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onLike: () -> Unit,
    onPass: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f)
            .shadow(24.dp, RoundedCornerShape(32.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
            .testTag("discover_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Visual Ambient Gradient based on genre compatibility
            val gradientBrush = remember(profile.favoriteGenre) {
                val colorAccent = when (profile.favoriteGenre.lowercase()) {
                    "rock" -> Color(0xFFE91E63)
                    "pop" -> Color(0xFF00BCD4)
                    "electronic" -> Color(0xFF9C27B0)
                    "classical" -> Color(0xFFFF9800)
                    else -> SpotifyGreen
                }
                Brush.verticalGradient(
                    colors = listOf(
                        colorAccent.copy(alpha = 0.15f),
                        CosmicSurface.copy(alpha = 0.6f),
                        CosmicBackground
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradientBrush)
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header - Compatibility percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left genre tag
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(100.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = profile.favoriteGenre.uppercase(),
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    // Compatibility Tag (Solid SpotifyGreen and black text)
                    Box(
                        modifier = Modifier
                            .background(SpotifyGreen, RoundedCornerShape(100.dp))
                            .shadow(8.dp, RoundedCornerShape(100.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "MATCH",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "%${profile.compatibilityPercentage}",
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Mid section: Spinning Vinyl disc of their signature song!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VinylDiscPlayer(
                        isPlaying = isPlaying,
                        onPlayToggle = onPlayToggle,
                        profileName = profile.name
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Signature Song translucent widget — tıklanınca kırpılmış kesit çalar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                            .clickable { onPlayToggle() }
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(SpotifyGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .border(1.dp, SpotifyGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = SpotifyGreenBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "İMZA ŞARKISI",
                                        color = SpotifyGreenBright,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp
                                    )
                                    Text(
                                        text = profile.signatureSongTitle,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = profile.signatureSongArtist,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isPlaying) {
                                SoundWaveformAnimation()
                            }
                        }
                    }
                }

                // Profile Details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        text = "${profile.name}, ${profile.age}",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile.bio,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = GlassBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Top artists row
                    Text(
                        text = "EN ÇOK DİNLEDİĞİ SANATÇILAR:",
                        color = SpotifyGreenBright,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = profile.topArtists,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Interactive Action Buttons matching the Design HTML
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pass Button
                    IconButton(
                        onClick = onPass,
                        modifier = Modifier
                            .size(64.dp)
                            .background(CosmicSurfaceElevated, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                            .testTag("swipe_left_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dislike",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play Preview Button
                    Button(
                        onClick = onPlayToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) SparkAccentPink else CosmicSurfaceElevated
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPlaying) SparkAccentPink.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Listen",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPlaying) "Durdur" else "Dinle",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Like Button with beautiful Spotify Green gradient feel and black heart icon
                    val likeButtonGradient = Brush.linearGradient(
                        colors = listOf(SpotifyGreen, SpotifyGreenBright)
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(16.dp, CircleShape, spotColor = SpotifyGreenBright, ambientColor = SpotifyGreen)
                            .background(likeButtonGradient, CircleShape)
                            .clickable { onLike() }
                            .testTag("swipe_right_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VinylDiscPlayer(
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    profileName: String
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val currentRotation = if (isPlaying) rotationAngle else 0f

    Box(
        modifier = Modifier
            .size(160.dp)
            .graphicsLayer { rotationZ = currentRotation }
            .shadow(12.dp, CircleShape)
            .background(Color.Black, CircleShape)
            .border(6.dp, CosmicSurfaceElevated, CircleShape)
            .clickable { onPlayToggle() },
        contentAlignment = Alignment.Center
    ) {
        // Vinyl grooves
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(color = Color(0xFF111111), radius = size.width / 2)
            drawCircle(color = Color(0xFF222222), radius = size.width / 2.4f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            drawCircle(color = Color(0xFF222222), radius = size.width / 3f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            drawCircle(color = Color(0xFF222222), radius = size.width / 4.5f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        }

        // Inner Sticker (Neon Colored)
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SpotifyGreenBright, SpotifyGreen, Color.Black)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = CosmicBackground,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SoundWaveformAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val heightScale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier
            .height(40.dp)
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(7) { index ->
            val heightMultiplier = when (index) {
                0 -> 0.4f
                1 -> 0.8f
                2 -> 1.0f
                3 -> 0.6f
                4 -> 0.9f
                5 -> 0.5f
                else -> 0.3f
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightScale * heightMultiplier)
                    .background(SpotifyGreenBright, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun MatchCelebrationDialog(
    profile: DiscoverProfile,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            // Neon Sparks Canvas backgrounds
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                // Draw neon circle highlights
                drawCircle(
                    color = SpotifyGreen.copy(alpha = 0.2f),
                    radius = size.width * 0.4f,
                    center = center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Aşkın Ritmi!",
                    color = SpotifyGreenBright,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Eşleşme Sağlandı! ⚡",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Avatar bubble & Vinyl matching animation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // User Vinyl
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(8.dp, CircleShape)
                            .background(CosmicSurfaceElevated, CircleShape)
                            .border(2.dp, SpotifyGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SEN", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text("⚡", fontSize = 32.sp)

                    Spacer(modifier = Modifier.width(16.dp))

                    // Partner Vinyl
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(8.dp, CircleShape)
                            .background(CosmicSurfaceElevated, CircleShape)
                            .border(2.dp, SparkAccentPink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.take(3).uppercase(),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "${profile.name} ile %${profile.compatibilityPercentage} müzikal uyumun var!",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Arka planda çalan parça: ${profile.signatureSongTitle}",
                    color = SpotifyGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "Sohbete Başla",
                        color = CosmicBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Kaydırmaya Devam Et",
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
