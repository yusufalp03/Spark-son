package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "me",
    val name: String = "Müzisyen Can",
    val age: Int = 24,
    val bio: String = "Müzik benim hayatım. Kadıköy sokaklarında gitar çalıyorum. Melodik metal ve synthwave hayranıyım.",
    val avatarUrl: String = "",
    val favoriteGenre: String = "Rock",
    val topArtists: String = "Metallica, Pink Floyd, Daft Punk, The Weeknd, Arctic Monkeys",
    val topTracks: String = "Starboy, Master of Puppets, Comfortably Numb, Get Lucky, Do I Wanna Know?",
    val signatureSongId: String = "spotify:track:596131",
    val signatureSongTitle: String = "Starboy",
    val signatureSongArtist: String = "The Weeknd",
    val signatureSongTrimStart: Float = 15f, // 15th second
    val signatureSongTrimEnd: Float = 45f // 45th second
)

@Entity(tableName = "discover_profile")
data class DiscoverProfile(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val bio: String,
    val avatarUrl: String,
    val favoriteGenre: String,
    val topArtists: String,
    val topTracks: String,
    val signatureSongId: String,
    val signatureSongTitle: String,
    val signatureSongArtist: String,
    val signatureSongTrimStart: Float = 10f,
    val signatureSongTrimEnd: Float = 40f,
    val compatibilityPercentage: Int,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val isMatched: Boolean = false
)

@Entity(tableName = "matches")
data class Match(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String,
    val matchedAt: Long = System.currentTimeMillis(),
    val lastMessage: String = "Harika bir parça!",
    val lastMessageTime: Long = System.currentTimeMillis()
)

enum class SyncStatus {
    SYNCED,
    PENDING_INSERT,
    PENDING_UPDATE,
    PENDING_DELETE,
    FAILED
}

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String, // Benzersiz UUID
    val matchId: String,
    val senderId: String, // "me" or other user
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

@Entity(tableName = "user_feedback")
data class UserFeedback(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_logs")
data class SystemLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tag: String,
    val message: String,
    val level: String, // "INFO", "WARNING", "ERROR"
    val timestamp: Long = System.currentTimeMillis()
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artist: String,
    val albumImageUrl: String = "",
    val previewUrl: String = ""
)
