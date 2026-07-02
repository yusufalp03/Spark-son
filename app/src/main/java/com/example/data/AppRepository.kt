package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {
    private val TAG = "AppRepository"

    // Yapılandırılmış tek scope: uygulama süreciyle yaşar, sign-out'ta işler iptal edilebilir.
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database = AppDatabase.getDatabase(context)
    private val userProfileDao = database.userProfileDao()
    private val discoverProfileDao = database.discoverProfileDao()
    private val matchDao = database.matchDao()
    private val chatMessageDao = database.chatMessageDao()
    private val userFeedbackDao = database.userFeedbackDao()

    val spotifyService = SpotifyService()
    val supabaseService = SupabaseService(context)
    val profileSyncService = ProfileSyncService(supabaseService, database)

    // Flows (Room = yerel tek doğruluk kaynağı)
    val myProfile: Flow<UserProfile?> = userProfileDao.getMyProfile()
    val discoverableProfiles: Flow<List<DiscoverProfile>> = discoverProfileDao.getDiscoverableProfiles()
    val allMatches: Flow<List<Match>> = matchDao.getAllMatches()

    fun getMessages(matchId: String): Flow<List<ChatMessage>> = chatMessageDao.getMessagesForMatch(matchId)

    fun currentUserId(): String? = supabaseService.currentUserId()

    // ---------- Profil ----------

    suspend fun saveUserProfile(profile: UserProfile) {
        userProfileDao.saveProfile(profile)
        // Buluttaki profil diğer kullanıcıların keşfinde göründüğü için eşitle
        val uploaded = supabaseService.upsertMyProfile(profile)
        if (!uploaded) {
            Log.w(TAG, "Profil buluta eşitlenemedi; yerel kayıt güncel.")
        }
    }

    // ---------- Keşif ----------

    suspend fun refreshDiscoverProfiles() = withContext(Dispatchers.IO) {
        try {
            val remoteProfiles = supabaseService.fetchDiscoverProfiles()
            if (remoteProfiles.isNotEmpty()) {
                discoverProfileDao.saveProfiles(remoteProfiles)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh discover profiles", e)
        }
    }

    // ---------- Swipe / Eşleşme ----------

    suspend fun swipeLeft(profileId: String) {
        val profile = discoverProfileDao.getProfileById(profileId) ?: return
        discoverProfileDao.updateProfile(profile.copy(isDisliked = true))
        val result = supabaseService.swipe(profileId, isLike = false)
        if (result.matchId != null) {
            // Teorik olarak olmaz; yine de tutarlılık için yut
            Log.w(TAG, "Unexpected match on dislike")
        }
    }

    /**
     * Sağa kaydırma: beğeni buluta yazılır; sunucu karşılıklı beğeni bulursa
     * eşleşme döner ve eşleşme listesi yenilenir.
     */
    suspend fun swipeRight(profileId: String): Boolean {
        val profile = discoverProfileDao.getProfileById(profileId) ?: return false
        discoverProfileDao.updateProfile(profile.copy(isLiked = true))

        val result = supabaseService.swipe(profileId, isLike = true)
        if (result.matched && result.matchId != null) {
            discoverProfileDao.updateProfile(profile.copy(isLiked = true, isMatched = true))
            matchDao.insertMatch(
                Match(
                    id = result.matchId,
                    userId = profile.id,
                    userName = profile.name,
                    userAvatarUrl = profile.avatarUrl,
                    lastMessage = "",
                    lastMessageTime = System.currentTimeMillis()
                )
            )
            return true
        }
        return false
    }

    suspend fun refreshMatches() = withContext(Dispatchers.IO) {
        try {
            val remoteMatches = supabaseService.fetchMyMatches()
            if (remoteMatches.isNotEmpty()) {
                matchDao.insertMatches(remoteMatches)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh matches", e)
        }
    }

    // ---------- Mesajlar (offline-first) ----------

    suspend fun sendChatMessage(matchId: String, text: String) {
        val senderId = currentUserId() ?: return
        val messageId = java.util.UUID.randomUUID().toString()
        val userMsg = ChatMessage(
            id = messageId,
            matchId = matchId,
            senderId = senderId,
            text = text,
            timestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_INSERT
        )
        chatMessageDao.insertMessage(userMsg)

        val match = matchDao.getMatchById(matchId)
        if (match != null) {
            matchDao.insertMatch(
                match.copy(lastMessage = text, lastMessageTime = System.currentTimeMillis())
            )
        }

        val isSuccess = supabaseService.syncMessageToCloud(userMsg)
        chatMessageDao.updateSyncStatus(
            messageId,
            if (isSuccess) SyncStatus.SYNCED else SyncStatus.FAILED
        )
    }

    suspend fun retryMessage(messageId: String) {
        val msg = chatMessageDao.getMessageById(messageId) ?: return
        chatMessageDao.updateSyncStatus(messageId, SyncStatus.PENDING_INSERT)

        val isSuccess = supabaseService.syncMessageToCloud(msg)
        chatMessageDao.updateSyncStatus(
            messageId,
            if (isSuccess) SyncStatus.SYNCED else SyncStatus.FAILED
        )

        if (isSuccess) {
            val match = matchDao.getMatchById(msg.matchId)
            if (match != null) {
                matchDao.insertMatch(
                    match.copy(lastMessage = msg.text, lastMessageTime = System.currentTimeMillis())
                )
            }
        }
    }

    /**
     * Sohbet açıldığında: geçmiş mesajları buluttan çekip Room'a yazar ve
     * realtime kanalına abone olur. Gelen her mesaj Room'a düşer; UI zaten
     * Room flow'unu dinlediği için otomatik güncellenir.
     */
    private var realtimeJob: Job? = null

    fun startChatSync(matchId: String) {
        stopChatSync()
        realtimeJob = repositoryScope.launch {
            try {
                val history = supabaseService.fetchMessages(matchId)
                if (history.isNotEmpty()) {
                    chatMessageDao.insertMessagesIfAbsent(history)
                }
                supabaseService.subscribeToMatchMessages(matchId).collect { message ->
                    chatMessageDao.insertMessagesIfAbsent(listOf(message))
                    val match = matchDao.getMatchById(matchId)
                    if (match != null) {
                        matchDao.insertMatch(
                            match.copy(lastMessage = message.text, lastMessageTime = message.timestamp)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Chat sync failed for $matchId", e)
            }
        }
    }

    fun stopChatSync() {
        realtimeJob?.cancel()
        realtimeJob = null
        repositoryScope.launch {
            supabaseService.unsubscribeFromMatchMessages()
        }
    }

    // ---------- Geri bildirim ----------

    suspend fun saveFeedback(email: String, rating: Int, comment: String) {
        val feedback = UserFeedback(email = email, rating = rating, comment = comment)
        userFeedbackDao.insertFeedback(feedback)
        val success = supabaseService.syncFeedbackToCloud(feedback)
        if (!success) {
            Log.w(TAG, "Feedback cloud sync failed; stored locally.")
        }
    }

    // ---------- Oturum kapatma ----------

    /** Çıkışta tüm yerel kullanıcı verisini temizler. */
    suspend fun clearLocalData() {
        stopChatSync()
        database.clearAllUserData()
    }
}
