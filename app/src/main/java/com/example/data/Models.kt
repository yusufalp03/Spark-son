package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "me",
    val name: String = "",
    val age: Int = 0,
    val bio: String = "",
    val avatarUrl: String = "",
    val favoriteGenre: String = "",
    val topArtists: String = "",
    val topTracks: String = "",
    val signatureSongId: String = "",
    val signatureSongTitle: String = "",
    val signatureSongArtist: String = "",
    val signatureSongAlbumArt: String = "",
    val signatureSongDurationMs: Long = 0L,
    // Kırpma aralığı: tam şarkı içindeki saniyeler (Spotify App Remote ile çalınır)
    val signatureSongTrimStart: Float = 0f,
    val signatureSongTrimEnd: Float = 30f
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
    val signatureSongAlbumArt: String = "",
    val signatureSongTrimStart: Float = 0f,
    val signatureSongTrimEnd: Float = 30f,
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
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis()
)

enum class SyncStatus {
    SYNCED,
    PENDING_INSERT,
    PENDING_UPDATE,
    PENDING_DELETE,
    FAILED
}

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["matchId", "timestamp"])]
)
data class ChatMessage(
    @PrimaryKey val id: String, // Benzersiz UUID
    val matchId: String,
    val senderId: String, // Supabase auth kullanıcı UUID'si
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

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artist: String,
    val albumImageUrl: String = "",
    val durationMs: Long = 0L
)
