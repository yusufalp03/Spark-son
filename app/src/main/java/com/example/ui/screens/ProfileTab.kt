package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.UserProfile
import com.example.ui.SparkViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileTab(
    viewModel: SparkViewModel,
    modifier: Modifier = Modifier
) {
    val myProfile by viewModel.myProfile.collectAsState()
    val searchResults by viewModel.currentTrackSearchResults.collectAsState()
    val isSearching by viewModel.isSearchingTracks.collectAsState()

    var name by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var favoriteGenre by remember { mutableStateOf("") }
    var topArtists by remember { mutableStateOf("") }
    var topTracks by remember { mutableStateOf("") }

    var searchQuery by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var showTrimmerDialog by remember { mutableStateOf(false) }

    // Synchronize inputs with state
    LaunchedEffect(myProfile) {
        myProfile?.let {
            name = it.name
            ageStr = it.age.toString()
            bio = it.bio
            favoriteGenre = it.favoriteGenre
            topArtists = it.topArtists
            topTracks = it.topTracks
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(SpotifyGreen.copy(alpha = 0.3f), CosmicSurface)
                            )
                        )
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val avatarUrl = myProfile?.avatarUrl.orEmpty()
                        if (avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Profil fotoğrafı",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, SpotifyGreen, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(SpotifyGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (name.isNotEmpty()) name.take(1).uppercase() else "S",
                                    color = CosmicBackground,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = name,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Spark Premium Üyesi",
                            color = SpotifyGreenBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Editable Profile details section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profil Bilgileri",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            if (isEditing) {
                                viewModel.updateProfileDetails(
                                    name,
                                    ageStr.toIntOrNull() ?: 24,
                                    bio,
                                    favoriteGenre,
                                    topArtists,
                                    topTracks
                                )
                            }
                            isEditing = !isEditing
                        },
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save" else "Edit",
                            tint = SpotifyGreenBright
                        )
                    }
                }
            }

            if (isEditing) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Adınız", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpotifyGreen,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = { ageStr = it },
                            label = { Text("Yaşınız", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpotifyGreen,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Hakkımda Biyografisi", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpotifyGreen,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        OutlinedTextField(
                            value = favoriteGenre,
                            onValueChange = { favoriteGenre = it },
                            label = { Text("Favori Müzik Türü", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpotifyGreen,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = topArtists,
                            onValueChange = { topArtists = it },
                            label = { Text("Favori Sanatçılar (Virgülle Ayırın)", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpotifyGreen,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmicSurface, RoundedCornerShape(16.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        ProfileInfoRow("Yaş", ageStr)
                        ProfileInfoRow("Hakkımda", bio)
                        ProfileInfoRow("Müzik Tarzı", favoriteGenre)
                        ProfileInfoRow("Sanatçılar", topArtists)
                    }
                }
            }

            // Signature Song display & trimmer action
            item {
                Text(
                    text = "İmza Şarkısı (Signature Song)",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                myProfile?.let { profile ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmicSurfaceElevated, RoundedCornerShape(16.dp))
                            .border(1.dp, SpotifyGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                if (profile.signatureSongAlbumArt.isNotBlank()) {
                                    AsyncImage(
                                        model = profile.signatureSongAlbumArt,
                                        contentDescription = "Albüm kapağı",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(SpotifyGreen.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = SpotifyGreenBright
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = profile.signatureSongTitle,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = profile.signatureSongArtist,
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { showTrimmerDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Süreyi Kırp", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Spotify music search catalog integration
            item {
                Text(
                    text = "İmza Şarkısı Ara & Değiştir",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchTracks(it)
                    },
                    label = { Text("Spotify'da Ara...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpotifyGreen,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spotify_search_input")
                )
            }

            if (isSearching) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SpotifyGreen)
                    }
                }
            } else {
                items(searchResults) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmicSurface, RoundedCornerShape(12.dp))
                            .clickable { viewModel.selectSignatureSong(track) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (track.albumImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = track.albumImageUrl,
                                contentDescription = "Albüm kapağı",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = track.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = track.artist, color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Sign Out / Oturumu Kapat Button
            item {
                Button(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp)
                        .height(50.dp)
                        .testTag("sign_out_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        text = "Spotify Oturumunu Kapat",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Trimmer dialogue overlay
        if (showTrimmerDialog) {
            SignatureSongTrimmerDialog(
                viewModel = viewModel,
                onDismiss = { showTrimmerDialog = false }
            )
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = SpotifyGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = value.ifEmpty { "Belirtilmemiş" }, color = TextPrimary, fontSize = 14.sp)
    }
}

/** Saniyeyi m:ss biçiminde gösterir (ör. 83 → 1:23). */
private fun formatSeconds(seconds: Float): String {
    val total = seconds.toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
fun SignatureSongTrimmerDialog(
    viewModel: SparkViewModel,
    onDismiss: () -> Unit
) {
    val myProfile by viewModel.myProfile.collectAsState()
    val activePlayingId by viewModel.activePlayingProfileId.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()

    val clipLength = SparkViewModel.CLIP_LENGTH_SECONDS
    // Süre bilinmiyorsa (eski kayıt) 4 dk varsay; Spotify şarkı sonunu kendisi sınırlar
    val songSeconds = myProfile?.signatureSongDurationMs
        ?.takeIf { it > 0 }?.let { it / 1000f } ?: 240f
    val maxStart = (songSeconds - clipLength).coerceAtLeast(0f)
    val isPreviewingClip = isAudioPlaying && activePlayingId == SparkViewModel.MY_TRIM_PREVIEW_ID

    var clipStart by remember {
        mutableStateOf((myProfile?.signatureSongTrimStart ?: 0f).coerceIn(0f, maxStart))
    }
    val clipEnd = (clipStart + clipLength).coerceAtMost(songSeconds)

    // Diyalog kapanınca çalan kesiti durdur
    DisposableEffect(Unit) {
        onDispose { viewModel.stopAudio() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "İmza Şarkısını Kırp",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Şarkının en can alıcı 30 saniyelik kesitini seçin. " +
                        "Keşfet ekranında profilinizi görenler bu kesiti dinler.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                Slider(
                    value = clipStart,
                    onValueChange = { clipStart = it.coerceIn(0f, maxStart) },
                    valueRange = 0f..maxStart.coerceAtLeast(1f),
                    enabled = maxStart > 0f,
                    colors = SliderDefaults.colors(
                        thumbColor = SpotifyGreenBright,
                        activeTrackColor = SpotifyGreen,
                        inactiveTrackColor = GlassBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Başlangıç: ${formatSeconds(clipStart)}",
                        color = SpotifyGreenBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bitiş: ${formatSeconds(clipEnd)} / ${formatSeconds(songSeconds)}",
                        color = SparkAccentPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.previewMySignatureClip(clipStart, clipEnd) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPreviewingClip) SparkAccentPink else CosmicSurface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isPreviewingClip) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPreviewingClip) "Durdur" else "Kesiti Dinle",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Dinleme, cihazınızdaki Spotify uygulaması üzerinden yapılır; " +
                        "kesit çalma Spotify Premium gerektirir.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateTrimRange(clipStart, clipEnd)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
            ) {
                Text("Kaydet", color = CosmicBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç", color = TextSecondary)
            }
        },
        containerColor = CosmicSurfaceElevated
    )
}
