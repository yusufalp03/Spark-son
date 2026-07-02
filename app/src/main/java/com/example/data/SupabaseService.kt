package com.example.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.FlowType
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.CodeVerifierCache
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString

// Uygulama genelinde paylaşılan tek OkHttp bağlantı havuzu.
object SharedHttp {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}

// Safe initialization helper for EncryptedSharedPreferences with hardware-backed Keystore
fun createEncryptedPrefs(context: Context, fileName: String): android.content.SharedPreferences {
    return try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Throwable) {
        Log.e("SupabaseService", "Failed to initialize EncryptedSharedPreferences for $fileName. Attempting recovery reset.", e)
        try {
            // Delete corrupt files/keys to prevent permanent app lockout
            context.deleteSharedPreferences(fileName)
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (ex: Throwable) {
            Log.e("SupabaseService", "Critical failure during EncryptedSharedPreferences recovery. Falling back to plain SharedPreferences.", ex)
            context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        }
    }
}

class AndroidCodeVerifierCache(context: Context) : CodeVerifierCache {
    private val prefs = createEncryptedPrefs(context, "supabase_code_verifier")

    override suspend fun saveCodeVerifier(codeVerifier: String) {
        withContext(Dispatchers.IO) {
            prefs.edit().putString("code_verifier", codeVerifier).commit()
        }
    }

    override suspend fun loadCodeVerifier(): String? {
        return withContext(Dispatchers.IO) {
            prefs.getString("code_verifier", null)
        }
    }

    override suspend fun deleteCodeVerifier() {
        withContext(Dispatchers.IO) {
            prefs.edit().remove("code_verifier").commit()
        }
    }
}

class AndroidSessionManager(context: Context) : SessionManager {
    private val prefs = createEncryptedPrefs(context, "supabase_session")
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        try {
            withContext(Dispatchers.IO) {
                val serialized = json.encodeToString(session)
                prefs.edit().putString("session_data", serialized).commit()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun loadSession(): UserSession? {
        return withContext(Dispatchers.IO) {
            val serialized = prefs.getString("session_data", null) ?: return@withContext null
            try {
                json.decodeFromString<UserSession>(serialized)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override suspend fun deleteSession() {
        withContext(Dispatchers.IO) {
            prefs.edit().remove("session_data").commit()
        }
    }
}

data class SwipeResult(val matched: Boolean, val matchId: String?)

class SupabaseService(private val context: Context) {
    private val TAG = "SupabaseService"

    private val supabaseUrl = BuildConfig.SUPABASE_URL.trim()
    private val supabaseKey = BuildConfig.SUPABASE_ANON_KEY.trim()

    fun isConfigured(): Boolean =
        supabaseUrl.startsWith("https://") &&
            !supabaseUrl.contains("proje-id") &&
            supabaseKey.isNotEmpty() &&
            !supabaseKey.contains("SUPABASE_ANON_KEY") &&
            !supabaseKey.contains("senin-anon-keyin")

    val supabaseClient: SupabaseClient by lazy {
        // Yapılandırma eksikse istemci yine de oluşturulur (uygulama açılışta
        // çökmesin diye) fakat tüm çağrılar başarısız olur; LoginScreen
        // kullanıcıya yapılandırma uyarısı gösterir.
        val url = if (isConfigured()) supabaseUrl else "https://invalid-not-configured.supabase.co"
        val key = if (isConfigured()) supabaseKey else "invalid"

        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Postgrest)
            install(Realtime)
            install(Auth) {
                host = "login"
                scheme = "spark"
                flowType = FlowType.PKCE
                sessionManager = AndroidSessionManager(context)
                codeVerifierCache = AndroidCodeVerifierCache(context)
            }
        }
    }

    private val httpClient: OkHttpClient = SharedHttp.client

    private fun userToken(): String? = supabaseClient.auth.currentAccessTokenOrNull()

    fun currentUserId(): String? = try {
        supabaseClient.auth.currentUserOrNull()?.id
    } catch (e: Exception) {
        null
    }

    private fun restRequest(path: String, body: String?, prefer: String? = null): Request? {
        val token = userToken() ?: return null
        val builder = Request.Builder()
            .url("$supabaseUrl$path")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
        if (prefer != null) builder.addHeader("Prefer", prefer)
        if (body != null) builder.post(body.toRequestBody("application/json".toMediaType()))
        return builder.build()
    }

    // ---------- Profil ----------

    suspend fun upsertMyProfile(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        val userId = currentUserId() ?: return@withContext false
        val body = JSONObject().apply {
            put("id", userId)
            put("name", profile.name)
            if (profile.age >= 18) put("age", profile.age) else put("age", JSONObject.NULL)
            put("bio", profile.bio)
            put("avatar_url", profile.avatarUrl)
            put("favorite_genre", profile.favoriteGenre)
            put("top_artists", profile.topArtists)
            put("top_tracks", profile.topTracks)
            put("signature_song_id", profile.signatureSongId)
            put("signature_song_title", profile.signatureSongTitle)
            put("signature_song_artist", profile.signatureSongArtist)
            put("signature_song_trim_start", profile.signatureSongTrimStart)
            put("signature_song_trim_end", profile.signatureSongTrimEnd)
        }.toString()
        val request = restRequest("/rest/v1/profiles", body, prefer = "resolution=merge-duplicates")
            ?: return@withContext false
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Profile upsert failed: ${response.code} ${response.body?.string()}")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Profile upsert exception", e)
            false
        }
    }

    // ---------- Keşif ----------

    suspend fun fetchDiscoverProfiles(limit: Int = 20): List<DiscoverProfile> = withContext(Dispatchers.IO) {
        val body = JSONObject().put("p_limit", limit).toString()
        val request = restRequest("/rest/v1/rpc/get_discover_profiles", body)
            ?: return@withContext emptyList()
        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Discover fetch failed: ${response.code} $responseBody")
                    return@withContext emptyList()
                }
                val array = JSONArray(responseBody)
                (0 until array.length()).map { i ->
                    val o = array.getJSONObject(i)
                    DiscoverProfile(
                        id = o.getString("id"),
                        name = o.optString("name"),
                        age = o.optInt("age", 0),
                        bio = o.optString("bio"),
                        avatarUrl = o.optString("avatar_url"),
                        favoriteGenre = o.optString("favorite_genre"),
                        topArtists = o.optString("top_artists"),
                        topTracks = o.optString("top_tracks"),
                        signatureSongId = o.optString("signature_song_id"),
                        signatureSongTitle = o.optString("signature_song_title"),
                        signatureSongArtist = o.optString("signature_song_artist"),
                        signatureSongTrimStart = o.optDouble("signature_song_trim_start", 15.0).toFloat(),
                        signatureSongTrimEnd = o.optDouble("signature_song_trim_end", 45.0).toFloat(),
                        compatibilityPercentage = o.optInt("compatibility", 60)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Discover fetch exception", e)
            emptyList()
        }
    }

    // ---------- Swipe / Eşleşme ----------

    suspend fun swipe(targetUserId: String, isLike: Boolean): SwipeResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("p_target", targetUserId)
            .put("p_is_like", isLike)
            .toString()
        val request = restRequest("/rest/v1/rpc/handle_swipe", body)
            ?: return@withContext SwipeResult(matched = false, matchId = null)
        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Swipe failed: ${response.code} $responseBody")
                    return@withContext SwipeResult(matched = false, matchId = null)
                }
                val array = JSONArray(responseBody)
                if (array.length() == 0) return@withContext SwipeResult(false, null)
                val o = array.getJSONObject(0)
                SwipeResult(
                    matched = o.optBoolean("matched", false),
                    matchId = if (o.isNull("match_id")) null else o.optString("match_id")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Swipe exception", e)
            SwipeResult(matched = false, matchId = null)
        }
    }

    suspend fun fetchMyMatches(): List<Match> = withContext(Dispatchers.IO) {
        val request = restRequest("/rest/v1/rpc/get_my_matches", "{}")
            ?: return@withContext emptyList()
        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Matches fetch failed: ${response.code} $responseBody")
                    return@withContext emptyList()
                }
                val array = JSONArray(responseBody)
                (0 until array.length()).map { i ->
                    val o = array.getJSONObject(i)
                    Match(
                        id = o.getString("match_id"),
                        userId = o.getString("other_id"),
                        userName = o.optString("other_name"),
                        userAvatarUrl = o.optString("other_avatar_url"),
                        matchedAt = parseTimestamp(o.optString("created_at")),
                        lastMessage = o.optString("last_message"),
                        lastMessageTime = parseTimestamp(o.optString("last_message_at"))
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Matches fetch exception", e)
            emptyList()
        }
    }

    // ---------- Mesajlar ----------

    suspend fun syncMessageToCloud(message: ChatMessage): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("id", message.id)
            put("match_id", message.matchId)
            put("sender_id", message.senderId)
            put("text", message.text)
        }.toString()
        val request = restRequest("/rest/v1/messages", body) ?: return@withContext false
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Message sync failed: ${response.code} ${response.body?.string()}")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Message sync exception", e)
            false
        }
    }

    suspend fun fetchMessages(matchId: String, limit: Int = 200): List<ChatMessage> = withContext(Dispatchers.IO) {
        val token = userToken() ?: return@withContext emptyList()
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/messages?match_id=eq.$matchId&order=created_at.asc&limit=$limit")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Messages fetch failed: ${response.code} $responseBody")
                    return@withContext emptyList()
                }
                val array = JSONArray(responseBody)
                (0 until array.length()).map { i ->
                    val o = array.getJSONObject(i)
                    ChatMessage(
                        id = o.getString("id"),
                        matchId = o.getString("match_id"),
                        senderId = o.getString("sender_id"),
                        text = o.optString("text"),
                        timestamp = parseTimestamp(o.optString("created_at")),
                        syncStatus = SyncStatus.SYNCED
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Messages fetch exception", e)
            emptyList()
        }
    }

    // ---------- Realtime ----------

    private var activeMessagesChannel: RealtimeChannel? = null

    /**
     * Aktif sohbet için gelen mesajları dinler. Yeni bir sohbete geçildiğinde
     * önce eski kanal kapatılır.
     */
    suspend fun subscribeToMatchMessages(matchId: String): Flow<ChatMessage> {
        unsubscribeFromMatchMessages()
        val realtimeChannel = supabaseClient.channel("messages-$matchId")
        val flow = realtimeChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter {
                eq("match_id", matchId)
            }
        }.mapNotNull { action -> parseRealtimeMessage(action.record) }
        activeMessagesChannel = realtimeChannel
        realtimeChannel.subscribe()
        return flow
    }

    suspend fun unsubscribeFromMatchMessages() {
        val channel = activeMessagesChannel ?: return
        activeMessagesChannel = null
        try {
            supabaseClient.realtime.removeChannel(channel)
        } catch (e: Exception) {
            Log.e(TAG, "Realtime unsubscribe failed", e)
        }
    }

    private fun parseRealtimeMessage(record: JsonObject): ChatMessage? {
        return try {
            ChatMessage(
                id = record["id"]?.jsonPrimitive?.contentOrNull ?: return null,
                matchId = record["match_id"]?.jsonPrimitive?.contentOrNull ?: return null,
                senderId = record["sender_id"]?.jsonPrimitive?.contentOrNull ?: "",
                text = record["text"]?.jsonPrimitive?.contentOrNull ?: "",
                timestamp = parseTimestamp(record["created_at"]?.jsonPrimitive?.contentOrNull ?: ""),
                syncStatus = SyncStatus.SYNCED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Realtime message parse failed", e)
            null
        }
    }

    // ---------- Geri bildirim ----------

    suspend fun syncFeedbackToCloud(feedback: UserFeedback): Boolean = withContext(Dispatchers.IO) {
        val userId = currentUserId() ?: return@withContext false
        val body = JSONObject().apply {
            put("user_id", userId)
            put("email", feedback.email)
            put("rating", feedback.rating)
            put("comment", feedback.comment)
        }.toString()
        val request = restRequest("/rest/v1/feedback", body) ?: return@withContext false
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Feedback sync failed: ${response.code} ${response.body?.string()}")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Feedback sync exception", e)
            false
        }
    }

    // ---------- Yardımcılar ----------

    companion object {
        // Postgres timestamptz (ör. 2026-07-02T10:00:00.123456+00:00) → epoch ms.
        // minSdk 24'te java.time olmadığı için saniye hassasiyetiyle parse edilir.
        fun parseTimestamp(iso: String): Long {
            if (iso.length < 19) return System.currentTimeMillis()
            return try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                format.parse(iso.substring(0, 19))?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
