package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 'me' LIMIT 1")
    fun getMyProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 'me' LIMIT 1")
    suspend fun getMyProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfile)

    @Query("DELETE FROM user_profile")
    suspend fun clearAll()
}

@Dao
interface DiscoverProfileDao {
    @Query("SELECT * FROM discover_profile WHERE isLiked = 0 AND isDisliked = 0")
    fun getDiscoverableProfiles(): Flow<List<DiscoverProfile>>

    @Query("SELECT * FROM discover_profile WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): DiscoverProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfiles(profiles: List<DiscoverProfile>)

    @Update
    suspend fun updateProfile(profile: DiscoverProfile)

    @Query("DELETE FROM discover_profile")
    suspend fun clearAll()
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY lastMessageTime DESC")
    fun getAllMatches(): Flow<List<Match>>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchById(matchId: String): Match?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: Match)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<Match>)

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatch(matchId: String)

    @Query("DELETE FROM matches")
    suspend fun clearAll()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE matchId = :matchId ORDER BY timestamp ASC")
    fun getMessagesForMatch(matchId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessagesIfAbsent(messages: List<ChatMessage>)

    @Query("UPDATE chat_messages SET syncStatus = :status WHERE id = :messageId")
    suspend fun updateSyncStatus(messageId: String, status: SyncStatus)

    @Query("SELECT * FROM chat_messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): ChatMessage?

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}

@Dao
interface UserFeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: UserFeedback)

    @Query("DELETE FROM user_feedback")
    suspend fun clearAll()
}

class Converters {
    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return enumValueOf<SyncStatus>(value)
    }

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String {
        return value.name
    }
}

@Database(
    entities = [
        UserProfile::class,
        DiscoverProfile::class,
        Match::class,
        ChatMessage::class,
        UserFeedback::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun discoverProfileDao(): DiscoverProfileDao
    abstract fun matchDao(): MatchDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun userFeedbackDao(): UserFeedbackDao

    suspend fun clearAllUserData() {
        userProfileDao().clearAll()
        discoverProfileDao().clearAll()
        matchDao().clearAll()
        chatMessageDao().clearAll()
        userFeedbackDao().clearAll()
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spark_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
