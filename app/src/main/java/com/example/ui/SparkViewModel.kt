package com.example.ui

import android.app.Application
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.components.ProfileSynthEngine
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.handleDeeplinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SparkViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "SparkViewModel"
    private val repository = AppRepository(application)
    private val synthEngine = ProfileSynthEngine()
    private var mediaPlayer: MediaPlayer? = null

    val authService = AuthService(repository.supabaseService.supabaseClient)

    // Auth State based on real session status or local demo/bypass login
    val isUserLoggedIn: StateFlow<Boolean> = combine(
        authService.mockUserLoggedIn,
        repository.supabaseService.supabaseClient.auth.sessionStatus
    ) { mockLogged, sessionStatus ->
        mockLogged || (sessionStatus is SessionStatus.Authenticated)
    }
    .catch { emit(false) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // UI States
    val myProfile: StateFlow<UserProfile?> = repository.myProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val discoverProfiles: StateFlow<List<DiscoverProfile>> = repository.discoverableProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matches: StateFlow<List<Match>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemLogs: StateFlow<List<SystemLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedbackList: StateFlow<List<UserFeedback>> = repository.allFeedback
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
        // Log every Supabase session status transition for debugging and analytics
        viewModelScope.launch {
            repository.supabaseService.supabaseClient.auth.sessionStatus.collect { status ->
                Log.d(TAG, "Supabase session status changed to: $status")
                val statusName = when (status) {
                    is SessionStatus.Authenticated -> "Authenticated"
                    is SessionStatus.NotAuthenticated -> "NotAuthenticated"
                    is SessionStatus.LoadingFromStorage -> "LoadingFromStorage"
                    else -> status.toString()
                }
                repository.logToDatabase("INFO", "Supabase session status: $statusName")
            }
        }

        // Automatically sync Spotify data and refresh discover queue when user logs in
        viewModelScope.launch {
            isUserLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    // Run Spotify sync and queue initialization asynchronously in background
                    launch(Dispatchers.IO) {
                        try {
                            val token = try {
                                // Attempt to extract real session access token or provider token if available
                                repository.supabaseService.supabaseClient.auth.currentSessionOrNull()?.accessToken ?: "mock_spotify_token"
                            } catch (e: Exception) {
                                "mock_spotify_token"
                            }
                            
                            // Attempt to sync Spotify data in background
                            repository.profileSyncService.syncSpotifyData(token)
                            
                            // Check if we already have discoverable profiles in local cache with a safety timeout
                            val cachedProfiles = withTimeoutOrNull(3000) {
                                repository.discoverableProfiles.firstOrNull()
                            } ?: emptyList()
                            
                            if (cachedProfiles.isEmpty()) {
                                Log.d(TAG, "Cached profiles empty. Auto-refreshing discover queue.")
                                refreshDiscoverQueue()
                            } else {
                                Log.d(TAG, "Discover queue has ${cachedProfiles.size} cached profiles. Skipping automatic API refresh.")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Background sync failed during startup", e)
                        }
                    }
                }
            }
        }
    }

    fun refreshDiscoverQueue() {
        viewModelScope.launch {
            _isDiscoverRefreshing.value = true
            repository.refreshDiscoverProfiles()
            _isDiscoverRefreshing.value = false
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
            val updated = current.copy(
                signatureSongId = track.id,
                signatureSongTitle = track.name,
                signatureSongArtist = track.artist
            )
            repository.saveUserProfile(updated)
            repository.logToDatabase("INFO", "Selected signature song: ${track.name} by ${track.artist}")
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
                // Show Match Celebration Screen!
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
    val isOfflineModeSimulated: StateFlow<Boolean> = repository.isOfflineModeSimulated
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setOfflineModeSimulated(simulated: Boolean) {
        repository.setOfflineModeSimulated(simulated)
    }

    fun getMessagesForMatch(matchId: String): Flow<List<ChatMessage>> {
        return repository.getMessages(matchId)
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

    // Audio & FM Synthesizer playback engine
    fun playAudioForProfile(profile: DiscoverProfile) {
        if (_activePlayingProfileId.value == profile.id && _isAudioPlaying.value) {
            stopAudio()
            return
        }

        stopAudio()
        _activePlayingProfileId.value = profile.id
        _isAudioPlaying.value = true

        // Try playing real preview URL if exists, else trigger FM Synthesizer!
        val previewUrl = "" // Empty in mock fallback
        if (previewUrl.isNotEmpty()) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(previewUrl)
                    prepareAsync()
                    setOnPreparedListener {
                        start()
                        viewModelScope.launch {
                            repository.logToDatabase("INFO", "Streaming preview URL for ${profile.name}'s signature song.")
                        }
                    }
                    setOnErrorListener { mp, _, _ ->
                        try {
                            mp.reset()
                            mp.release()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error releasing MediaPlayer on error path", e)
                        }
                        if (mediaPlayer == mp) {
                            mediaPlayer = null
                        }
                        // Fall back to synth on network stream error
                        synthEngine.start(profile.favoriteGenre)
                        viewModelScope.launch {
                            repository.logToDatabase("INFO", "Preview stream failed. Synthesizing ${profile.favoriteGenre} locally.")
                        }
                        true
                    }
                }
            } catch (e: Exception) {
                synthEngine.start(profile.favoriteGenre)
            }
        } else {
            synthEngine.start(profile.favoriteGenre)
            viewModelScope.launch {
                repository.logToDatabase("INFO", "FM Synthesising '${profile.favoriteGenre}' melody for ${profile.name}'s signature song.")
            }
        }
    }

    fun stopAudio() {
        _isAudioPlaying.value = false
        _activePlayingProfileId.value = null
        try {
            synthEngine.stop()
            mediaPlayer?.let { player ->
                try {
                    player.stop()
                } catch (e: Exception) {
                    // Suppress if already stopped or in invalid state
                }
                player.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio: ${e.message}")
        }
    }

    fun clearSystemLogs() {
        viewModelScope.launch {
            repository.clearSystemLogs()
        }
    }

    fun handleAuthDeeplink(intent: Intent) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.supabaseService.supabaseClient.handleDeeplinks(intent)
                }
                repository.logToDatabase("INFO", "Successfully handled Spotify/Supabase auth deeplink redirect.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle deep link", e)
                repository.logToDatabase("ERROR", "Failed to parse deep link: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }
}
