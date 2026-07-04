package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.ui.components.ProfileSynthEngine
import com.example.ui.components.SpotifyRemotePlayer
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.handleDeeplinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Oturum durumu: açılışta Loading gösterilir, login ekranı "parlamaz". */
enum class AuthUiState { Loading, LoggedIn, LoggedOut }

class SparkViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "SparkViewModel"
    private val repository = AppRepository(application)
    private val synthEngine = ProfileSynthEngine()
    private val remotePlayer = SpotifyRemotePlayer(
        clientId = BuildConfig.SPOTIFY_CLIENT_ID,
        redirectUri = "spark://login"
    )

    val authService = AuthService(repository.supabaseService.supabaseClient)

    val myUserId: String?
        get() = repository.currentUserId()

    val authState: StateFlow<AuthUiState> =
        repository.supabaseService.supabaseClient.auth.sessionStatus
            .map { status ->
                when (status) {
                    is SessionStatus.Authenticated -> AuthUiState.LoggedIn
                    is SessionStatus.LoadingFromStorage -> AuthUiState.Loading
                    else -> AuthUiState.LoggedOut
                }
            }
            .catch { emit(AuthUiState.LoggedOut) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, AuthUiState.Loading)

    // UI States
    val myProfile: StateFlow<UserProfile?> = repository.myProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val discoverProfiles: StateFlow<List<DiscoverProfile>> = repository.discoverableProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matches: StateFlow<List<Match>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isDiscoverRefreshing = MutableStateFlow(false)
    val isDiscoverRefreshing = _isDiscoverRefreshing.asStateFlow()

    private val _currentTrackSearchResults = MutableStateFlow<List<SpotifyTrack>>(emptyList())
    val currentTrackSearchResults = _currentTrackSearchResults.asStateFlow()

    private val _isSearchingTracks = MutableStateFlow(false)
    val isSearchingTracks = _isSearchingTracks.asStateFlow()

    private val _matchCelebrationProfile = MutableStateFlow<DiscoverProfile?>(null)
    val matchCelebrationProfile = _matchCelebrationProfile.asStateFlow()

    private val _activePlayingProfileId = MutableStateFlow<String?>(null)
    val activePlayingProfileId = _activePlayingProfileId.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying = _isAudioPlaying.asStateFlow()

    init {
        // Girişten sonra: profil satırını garanti et, Spotify verisini eşitle,
        // keşif kuyruğunu ve eşleşmeleri yenile.
        viewModelScope.launch {
            authState.collect { state ->
                if (state == AuthUiState.LoggedIn) {
                    launch(Dispatchers.IO) {
                        try {
                            ensureProfileRow()

                            // Spotify API'si için Supabase JWT'si değil,
                            // OAuth'tan gelen provider token gerekir.
                            val providerToken = try {
                                repository.supabaseService.supabaseClient.auth
                                    .currentSessionOrNull()?.providerToken
                            } catch (e: Exception) {
                                null
                            }
                            repository.profileSyncService.syncSpotifyData(providerToken)

                            repository.refreshMatches()

                            val cachedProfiles = withTimeoutOrNull(3000) {
                                repository.discoverableProfiles.firstOrNull()
                            } ?: emptyList()
                            if (cachedProfiles.isEmpty()) {
                                refreshDiscoverQueue()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Background sync failed after login", e)
                        }
                    }
                }
            }
        }
    }

    /**
     * profiles tablosunda satırımız yoksa oluşturur. likes/matches tabloları
     * bu satıra foreign key ile bağlı olduğundan swipe öncesi şarttır.
     */
    private suspend fun ensureProfileRow() {
        val userInfo = authService.currentUser
        val metaName = userInfo?.userMetadata?.get("name")?.toString()?.trim('"') ?: ""
        val metaAvatar = userInfo?.userMetadata?.get("avatar_url")?.toString()?.trim('"') ?: ""

        val existing = repository.myProfile.firstOrNull()
        val profile = existing ?: UserProfile(name = metaName, avatarUrl = metaAvatar)
        repository.saveUserProfile(profile)
    }

    fun refreshDiscoverQueue() {
        viewModelScope.launch {
            _isDiscoverRefreshing.value = true
            repository.refreshDiscoverProfiles()
            _isDiscoverRefreshing.value = false
        }
    }

    fun refreshMatches() {
        viewModelScope.launch {
            repository.refreshMatches()
        }
    }

    fun searchTracks(query: String) {
        if (query.isBlank()) {
            _currentTrackSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearchingTracks.value = true
            val results = repository.spotifyService.searchTracks(query)
            _currentTrackSearchResults.value = results
            _isSearchingTracks.value = false
        }
    }

    fun selectSignatureSong(track: SpotifyTrack) {
        viewModelScope.launch {
            val current = myProfile.value ?: UserProfile()
            val songSeconds = track.durationMs / 1000f
            val clipEnd = if (songSeconds > 0f) minOf(CLIP_LENGTH_SECONDS, songSeconds) else CLIP_LENGTH_SECONDS
            val updated = current.copy(
                signatureSongId = track.id,
                signatureSongTitle = track.name,
                signatureSongArtist = track.artist,
                signatureSongAlbumArt = track.albumImageUrl,
                signatureSongDurationMs = track.durationMs,
                // Yeni şarkıda kesit şarkının başına sıfırlanır
                signatureSongTrimStart = 0f,
                signatureSongTrimEnd = clipEnd
            )
            repository.saveUserProfile(updated)
        }
    }

    fun updateProfileDetails(name: String, age: Int, bio: String, genre: String, artists: String, tracks: String) {
        viewModelScope.launch {
            val current = myProfile.value ?: UserProfile()
            val updated = current.copy(
                name = name,
                age = age,
                bio = bio,
                favoriteGenre = genre,
                topArtists = artists,
                topTracks = tracks
            )
            repository.saveUserProfile(updated)
        }
    }

    fun updateTrimRange(start: Float, end: Float) {
        viewModelScope.launch {
            val current = myProfile.value ?: UserProfile()
            val updated = current.copy(
                signatureSongTrimStart = start,
                signatureSongTrimEnd = end
            )
            repository.saveUserProfile(updated)
        }
    }

    fun swipeLeft(profileId: String) {
        stopAudio()
        viewModelScope.launch {
            repository.swipeLeft(profileId)
        }
    }

    fun swipeRight(profileId: String) {
        stopAudio()
        viewModelScope.launch {
            val matched = repository.swipeRight(profileId)
            if (matched) {
                val profile = discoverProfiles.value.find { it.id == profileId }
                if (profile != null) {
                    _matchCelebrationProfile.value = profile
                    playAudioForProfile(profile)
                }
            }
        }
    }

    fun dismissMatchCelebration() {
        stopAudio()
        _matchCelebrationProfile.value = null
    }

    // Messages
    fun getMessagesForMatch(matchId: String): Flow<List<ChatMessage>> {
        return repository.getMessages(matchId)
    }

    /** Sohbet ekranı açıldığında çağrılır: geçmişi çeker + realtime dinler. */
    fun openChat(matchId: String) {
        repository.startChatSync(matchId)
    }

    fun closeChat() {
        repository.stopChatSync()
    }

    fun sendChatMessage(matchId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendChatMessage(matchId, text)
        }
    }

    fun retryChatMessage(messageId: String) {
        viewModelScope.launch {
            repository.retryMessage(messageId)
        }
    }

    // Feedback
    fun sendUserFeedback(email: String, rating: Int, comment: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveFeedback(email, rating, comment)
            onSuccess()
        }
    }

    // Audio: imza şarkısının kırpılmış kesiti cihazdaki Spotify uygulamasıyla
    // (App Remote, Premium gerektirir) tam şarkının içinden çalınır. Spotify
    // yoksa veya çalma reddedilirse türe göre FM synth'e geri düşülür.
    // [context] Activity olmalı: App Remote yetkilendirme ekranını bu context
    // üzerinden açar (Application context ile açılamaz).
    fun playAudioForProfile(context: Context, profile: DiscoverProfile) {
        if (_activePlayingProfileId.value == profile.id && _isAudioPlaying.value) {
            stopAudio()
            return
        }

        stopAudio()
        _activePlayingProfileId.value = profile.id
        _isAudioPlaying.value = true
        if (profile.signatureSongId.isNotBlank()) {
            remotePlayer.playClip(
                context = context,
                trackId = profile.signatureSongId,
                startSec = profile.signatureSongTrimStart,
                endSec = profile.signatureSongTrimEnd,
                onFinished = { onClipFinished(profile.id) },
                onError = {
                    // Kart hâlâ çalıyor görünüyorsa synth'e geri düş
                    if (_activePlayingProfileId.value == profile.id && _isAudioPlaying.value) {
                        synthEngine.start(profile.favoriteGenre)
                    }
                }
            )
        } else {
            synthEngine.start(profile.favoriteGenre)
        }
    }

    /** Kırpma diyaloğu: kendi imza şarkısının seçilen kesitini dinletir/durdurur. */
    fun previewMySignatureClip(context: Context, startSec: Float, endSec: Float) {
        if (_activePlayingProfileId.value == MY_TRIM_PREVIEW_ID && _isAudioPlaying.value) {
            stopAudio()
            return
        }
        val trackId = myProfile.value?.signatureSongId.orEmpty()
        if (trackId.isBlank()) return

        stopAudio()
        _activePlayingProfileId.value = MY_TRIM_PREVIEW_ID
        _isAudioPlaying.value = true
        remotePlayer.playClip(
            context = context,
            trackId = trackId,
            startSec = startSec,
            endSec = endSec,
            onFinished = { onClipFinished(MY_TRIM_PREVIEW_ID) },
            onError = { onClipFinished(MY_TRIM_PREVIEW_ID) }
        )
    }

    private fun onClipFinished(profileId: String) {
        if (_activePlayingProfileId.value == profileId) {
            _isAudioPlaying.value = false
            _activePlayingProfileId.value = null
        }
    }

    fun stopAudio() {
        _isAudioPlaying.value = false
        _activePlayingProfileId.value = null
        remotePlayer.stop()
        try {
            synthEngine.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio: ${e.message}")
        }
    }

    fun handleAuthDeeplink(intent: Intent) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.supabaseService.supabaseClient.handleDeeplinks(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle deep link", e)
            }
        }
    }

    fun signOut() {
        stopAudio()
        viewModelScope.launch {
            try {
                authService.signOut()
            } finally {
                repository.clearLocalData()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
        remotePlayer.release()
        repository.stopChatSync()
    }

    companion object {
        /** İmza şarkısı kesitinin uzunluğu (Instagram notu gibi sabit pencere). */
        const val CLIP_LENGTH_SECONDS = 30f

        /** Kırpma diyaloğundaki "kendi kesitim" çalması için sahte profil kimliği. */
        const val MY_TRIM_PREVIEW_ID = "my_trim_preview"
    }
}
