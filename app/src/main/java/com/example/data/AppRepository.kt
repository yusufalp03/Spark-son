package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AppRepository(private val context: Context) {
    private val TAG = "AppRepository"
    
    private val database = AppDatabase.getDatabase(context)
    private val userProfileDao = database.userProfileDao()
    private val discoverProfileDao = database.discoverProfileDao()
    private val matchDao = database.matchDao()
    private val chatMessageDao = database.chatMessageDao()
    private val userFeedbackDao = database.userFeedbackDao()
    private val systemLogDao = database.systemLogDao()

    val spotifyService = SpotifyService(database)
    val supabaseService = SupabaseService(database, context)
    val profileSyncService = ProfileSyncService(supabaseService.supabaseClient, database)

    private val _isOfflineModeSimulated = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isOfflineModeSimulated: kotlinx.coroutines.flow.StateFlow<Boolean> = _isOfflineModeSimulated

    fun setOfflineModeSimulated(simulated: Boolean) {
        _isOfflineModeSimulated.value = simulated
        CoroutineScope(Dispatchers.IO).launch {
            logToDatabase("INFO", "Simulated Offline Mode changed to: $simulated")
        }
    }

    // Flows
    val myProfile: Flow<UserProfile?> = userProfileDao.getMyProfile()
    val discoverableProfiles: Flow<List<DiscoverProfile>> = discoverProfileDao.getDiscoverableProfiles()
    val allMatches: Flow<List<Match>> = matchDao.getAllMatches()
    val recentLogs: Flow<List<SystemLog>> = systemLogDao.getRecentLogs()
    val allFeedback: Flow<List<UserFeedback>> = userFeedbackDao.getAllFeedback()

    fun getMessages(matchId: String): Flow<List<ChatMessage>> = chatMessageDao.getMessagesForMatch(matchId)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            // Seed default profile if empty
            val profile = userProfileDao.getMyProfileDirect()
            if (profile == null) {
                userProfileDao.saveProfile(UserProfile())
                logToDatabase("INFO", "Initialized default user profile.")
            }
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        userProfileDao.saveProfile(profile)
        logToDatabase("INFO", "Updated user profile. Name: ${profile.name}, Genre: ${profile.favoriteGenre}")
    }

    suspend fun refreshDiscoverProfiles() = withContext(Dispatchers.IO) {
        try {
            val myProfile = userProfileDao.getMyProfileDirect() ?: UserProfile()
            logToDatabase("INFO", "Refreshing discovery queue based on user music profile.")
            val remoteProfiles = supabaseService.fetchDiscoverProfiles(
                myProfile.favoriteGenre,
                myProfile.topArtists
            )
            if (remoteProfiles.isNotEmpty()) {
                discoverProfileDao.saveProfiles(remoteProfiles)
                logToDatabase("INFO", "Successfully loaded and saved ${remoteProfiles.size} discovery profiles.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh discover profiles", e)
            logToDatabase("ERROR", "Refresh Discover Profiles Exception: ${e.message}")
        }
    }

    suspend fun swipeLeft(profileId: String) {
        val profile = discoverProfileDao.getProfileById(profileId) ?: return
        val updated = profile.copy(isDisliked = true)
        discoverProfileDao.updateProfile(updated)
        logToDatabase("INFO", "User skipped candidate: ${profile.name}")
    }

    suspend fun swipeRight(profileId: String): Boolean {
        val profile = discoverProfileDao.getProfileById(profileId) ?: return false
        val updated = profile.copy(isLiked = true)
        discoverProfileDao.updateProfile(updated)
        logToDatabase("INFO", "User liked candidate: ${profile.name}")

        // 80% chance of matching to keep the app highly engaging and functional!
        val isMatch = profile.compatibilityPercentage >= 60
        if (isMatch) {
            val matchedProfile = updated.copy(isMatched = true)
            discoverProfileDao.updateProfile(matchedProfile)

            val match = Match(
                id = "match_${profile.id}",
                userId = profile.id,
                userName = profile.name,
                userAvatarUrl = profile.avatarUrl,
                lastMessage = "Müzikal eşleşme kuruldu! İlk mesajı sen gönder.",
                lastMessageTime = System.currentTimeMillis()
            )
            matchDao.insertMatch(match)
            logToDatabase("INFO", "🚨 Eşleşme Gerçekleşti! Spark ortağı: ${profile.name} (%${profile.compatibilityPercentage} uyum)")

            // Auto greeting message
            val systemMsg = ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                matchId = match.id,
                senderId = profile.id,
                text = "Hey! Müzikal uyumumuz %${profile.compatibilityPercentage} çıktı. Profilindeki '${profile.signatureSongTitle}' şarkısını çok severim! 🎵🎸",
                timestamp = System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED
            )
            chatMessageDao.insertMessage(systemMsg)
            return true
        }
        return false
    }

    suspend fun sendChatMessage(matchId: String, text: String) {
        val messageId = java.util.UUID.randomUUID().toString()
        val userMsg = ChatMessage(
            id = messageId,
            matchId = matchId,
            senderId = "me",
            text = text,
            timestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_INSERT
        )
        chatMessageDao.insertMessage(userMsg)

        // Find the partner match details
        val matchesList = allMatches.firstOrNull() ?: emptyList()
        val match = matchesList.find { it.id == matchId } ?: return

        // Update last message in Match table
        val updatedMatch = match.copy(
            lastMessage = text,
            lastMessageTime = System.currentTimeMillis()
        )
        matchDao.insertMatch(updatedMatch)
        logToDatabase("INFO", "Sohbet mesajı gönderiliyor (UUID: $messageId): ${match.userName}")

        // Attempt Supabase sync
        try {
            if (_isOfflineModeSimulated.value) {
                throw Exception("Çevrimdışı Mod Simülasyonu Etkin")
            }
            
            val isSuccess = supabaseService.syncMessageToCloud(userMsg)
            if (!isSuccess) {
                throw Exception("Supabase bulut eşitleme hatası")
            }
            
            // Mark as synced
            chatMessageDao.updateSyncStatus(messageId, SyncStatus.SYNCED)
            logToDatabase("INFO", "Sohbet mesajı bulutla eşitlendi (SYNCED)")

            // Trigger organic reply
            triggerAutomatedReply(matchId, text, updatedMatch)
        } catch (e: Exception) {
            chatMessageDao.updateSyncStatus(messageId, SyncStatus.FAILED)
            logToDatabase("WARNING", "Mesaj gönderimi başarısız oldu (Çevrimdışı/Hata): ${e.message}")
        }
    }

    suspend fun retryMessage(messageId: String) {
        val msg = chatMessageDao.getMessageById(messageId) ?: return
        chatMessageDao.updateSyncStatus(messageId, SyncStatus.PENDING_INSERT)
        logToDatabase("INFO", "Mesaj yeniden gönderilmeye çalışılıyor: $messageId")

        val matchesList = allMatches.firstOrNull() ?: emptyList()
        val match = matchesList.find { it.id == msg.matchId } ?: return

        try {
            if (_isOfflineModeSimulated.value) {
                throw Exception("Çevrimdışı Mod Simülasyonu Etkin")
            }
            
            val isSuccess = supabaseService.syncMessageToCloud(msg)
            if (!isSuccess) {
                throw Exception("Supabase bulut eşitleme hatası")
            }

            chatMessageDao.updateSyncStatus(messageId, SyncStatus.SYNCED)
            logToDatabase("INFO", "Mesaj başarıyla yeniden gönderildi ve bulutla eşitlendi (SYNCED)")

            // Update last message in match info
            val updatedMatch = match.copy(
                lastMessage = msg.text,
                lastMessageTime = System.currentTimeMillis()
            )
            matchDao.insertMatch(updatedMatch)

            // Trigger organic reply
            triggerAutomatedReply(msg.matchId, msg.text, updatedMatch)
        } catch (e: Exception) {
            chatMessageDao.updateSyncStatus(messageId, SyncStatus.FAILED)
            logToDatabase("WARNING", "Mesaj yeniden gönderimi başarısız oldu: ${e.message}")
        }
    }

    private fun triggerAutomatedReply(matchId: String, userText: String, updatedMatch: Match) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(1500)
            val replyText = generateAutomatedReply(updatedMatch.userName, userText)
            val replyMsg = ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                matchId = matchId,
                senderId = updatedMatch.userId,
                text = replyText,
                timestamp = System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED
            )
            chatMessageDao.insertMessage(replyMsg)

            // Update last message
            val finalMatch = updatedMatch.copy(
                lastMessage = replyText,
                lastMessageTime = System.currentTimeMillis()
            )
            matchDao.insertMatch(finalMatch)
            logToDatabase("INFO", "Received automated reply from match: ${updatedMatch.userName}")
        }
    }

    private fun generateAutomatedReply(name: String, userMessage: String): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("merhaba") || lower.contains("selam") || lower.contains("hey") -> {
                "Selam! Nasıl gidiyor? Bugün en son hangi şarkıyı dinledin? 😊"
            }
            lower.contains("nasılsın") || lower.contains("ne haber") -> {
                "Harikayım! Spotify listelerimi güncelliyordum. Sen nasılsın, neler yapıyorsun?"
            }
            lower.contains("şarkı") || lower.contains("müzik") || lower.contains("dinle") -> {
                "Evet, müzik gerçekten ruhun gıdası! Ben de şu an ritme ayak uyduruyorum. Seninle ortak şarkılarımızın olması çok tatlı."
            }
            lower.contains("gitar") || lower.contains("enstrüman") || lower.contains("konser") -> {
                "Aaa harika! Ben de hep canlı konserlere gitmeye bayılırım. Atmosferi bambaşka oluyor!"
            }
            else -> {
                "Gerçekten mi? Bu çok ilginç! Peki bu tarz müzikler dışında en çok hangi janrları seversin? 🎧⚡"
            }
        }
    }

    suspend fun saveFeedback(email: String, rating: Int, comment: String) {
        val feedback = UserFeedback(email = email, rating = rating, comment = comment)
        userFeedbackDao.insertFeedback(feedback)
        logToDatabase("INFO", "Saved user feedback locally.")
        
        val success = supabaseService.syncFeedbackToCloud(feedback)
        if (success) {
            logToDatabase("INFO", "Feedback successfully synced to cloud storage.")
        } else {
            logToDatabase("WARNING", "Feedback cloud sync delayed. Stored in offline buffer.")
        }
    }

    suspend fun clearSystemLogs() {
        systemLogDao.clearLogs()
        logToDatabase("INFO", "System logs database cleared by Admin.")
    }

    suspend fun logToDatabase(level: String, message: String) {
        try {
            systemLogDao.insertLog(
                SystemLog(tag = TAG, message = message, level = level)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Database logging failed: ${e.message}")
        }
    }
}
