package com.example.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.CodeVerifierCache
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

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

class SupabaseService(private val database: AppDatabase, private val context: Context) {
    private val TAG = "SupabaseService"
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // Real Supabase Client Initialization (Kotlin SDK)
    val supabaseClient: SupabaseClient by lazy {
        val rawUrl = BuildConfig.SUPABASE_URL.trim()
        val finalUrl = if (rawUrl.isEmpty() || rawUrl.contains("proje-id") || !rawUrl.startsWith("http")) {
            "https://lfiljzqqbmkkimlwdyfm.supabase.co" // Safe fallback URL format
        } else {
            rawUrl
        }

        val rawKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        val finalKey = if (rawKey.isEmpty() || rawKey.contains("senin-anon-keyin")) {
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxmaWxqenFxYm1ra2ltbHdkeWZtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI4Mzk3ODAsImV4cCI6MjA5ODQxNTc4MH0.4LY5WR7IyaUdDulAKAp_qnlQBikE7IxI4rVa-fWQfDI"
        } else {
            rawKey
        }

        createSupabaseClient(
            supabaseUrl = finalUrl,
            supabaseKey = finalKey
        ) {
            install(Postgrest)
            install(Realtime)
            install(Auth) {
                host = "login"
                scheme = "spark"
                sessionManager = AndroidSessionManager(context)
                codeVerifierCache = AndroidCodeVerifierCache(context)
            }
        }
    }


    // Gemini Client for generating rich, dynamic organic profiles
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun fetchDiscoverProfiles(userGenre: String, userArtists: String): List<DiscoverProfile> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        if (apiKey.isEmpty() || apiKey.contains("MY_GEMINI_API_KEY")) {
            logToDatabase("WARNING", "Gemini API Key missing. Seeding local curated profiles.")
            return@withContext getCuratedBackupProfiles(userGenre)
        }

        val prompt = """
            Giriş yapan kullanıcının sevdiği müzik tarzı "$userGenre" ve sevdiği sanatçılar "$userArtists".
            Bu verilere dayanarak, kullanıcıya müzikal uyum sağlayabilecek, müzik zevkini merkezine alan 6 farklı flört adayı profili oluştur.
            Yanıtı kesinlikle sadece saf bir JSON array formatında döndür. Hiçbir açıklama, markdown işareti veya ek metin ekleme.
            Format tam olarak şu şekilde olmalıdır:
            [
              {
                "id": "benzersiz_id_1",
                "name": "İsim",
                "age": 23,
                "bio": "Müzik zevkiyle harmanlanmış Türkçe biyografi yazısı.",
                "avatarUrl": "",
                "favoriteGenre": "Pop veya Rock veya Electronic vb.",
                "topArtists": "Sanatçı1, Sanatçı2, Sanatçı3",
                "topTracks": "Şarkı1, Şarkı2, Şarkı3",
                "signatureSongId": "spotify:track:rastgeleid",
                "signatureSongTitle": "İmza Şarkısı Adı",
                "signatureSongArtist": "İmza Şarkısı Sanatçısı",
                "signatureSongTrimStart": 15.0,
                "signatureSongTrimEnd": 45.0,
                "compatibilityPercentage": 85
              }
            ]
            
            Kurallar:
            1. İsimler Türkçe olmalıdır (Melis, Kaan, Zeynep, Tolga, İrem, Berk vb.).
            2. Yaşlar 20 ile 32 arasında olmalıdır.
            3. compatibilityPercentage (Uyum yüzdesi) kullanıcının müzik zevkine göre hesaplanmış 50-98 arası gerçekçi bir tam sayı olmalıdır.
            4. Biyografiler eğlenceli, samimi ve müzik odaklı Türkçe olmalıdır.
        """.trimIndent()

        try {
            // Setup Gemini REST request
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            
            val jsonRequest = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful && responseBody.isNotEmpty()) {
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.getJSONArray("candidates")
                val textResponse = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                // Parse json array
                val listType = Types.newParameterizedType(List::class.java, DiscoverProfile::class.java)
                val adapter = moshi.adapter<List<DiscoverProfile>>(listType)
                val profiles = adapter.fromJson(textResponse) ?: emptyList()
                
                logToDatabase("INFO", "Gemini API successfully generated ${profiles.size} organic profiles.")
                profiles
            } else {
                Log.e(TAG, "Gemini API call failed with code ${response.code}: $responseBody")
                logToDatabase("ERROR", "Gemini API error code: ${response.code}")
                getCuratedBackupProfiles(userGenre)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini profile generation", e)
            logToDatabase("WARNING", "Gemini API Generation Exception: ${e.message}. Seeding backup catalog.")
            getCuratedBackupProfiles(userGenre)
        }
    }

    suspend fun syncFeedbackToCloud(feedback: UserFeedback): Boolean = withContext(Dispatchers.IO) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val supabaseKey = BuildConfig.SUPABASE_ANON_KEY

        try {
            val url = "$supabaseUrl/rest/v1/user_feedback"
            val jsonBody = JSONObject().apply {
                put("email", feedback.email)
                put("rating", feedback.rating)
                put("comment", feedback.comment)
                put("timestamp", feedback.timestamp)
            }
            
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                logToDatabase("INFO", "Feedback successfully uploaded to real Supabase! Response: $responseBody")
                true
            } else {
                Log.e(TAG, "Supabase feedback upload failed: ${response.code} $responseBody")
                logToDatabase("ERROR", "Supabase feedback upload failed with code: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Supabase feedback upload", e)
            logToDatabase("WARNING", "Supabase sync exception: ${e.message}")
            false
        }
    }

    suspend fun syncMessageToCloud(message: ChatMessage): Boolean = withContext(Dispatchers.IO) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val supabaseKey = BuildConfig.SUPABASE_ANON_KEY

        try {
            val url = "$supabaseUrl/rest/v1/chat_messages"
            val jsonBody = JSONObject().apply {
                put("id", message.id)
                put("matchId", message.matchId)
                put("senderId", message.senderId)
                put("text", message.text)
                put("timestamp", message.timestamp)
            }
            
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                logToDatabase("INFO", "Message synced to real Supabase cloud (ID: ${message.id})!")
                true
            } else {
                Log.e(TAG, "Supabase message sync failed: ${response.code} $responseBody")
                logToDatabase("ERROR", "Supabase message sync failed with code: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Supabase message sync", e)
            logToDatabase("WARNING", "Supabase message sync exception: ${e.message}")
            false
        }
    }

    private suspend fun logToDatabase(level: String, message: String) {
        try {
            database.systemLogDao().insertLog(
                SystemLog(tag = TAG, message = message, level = level)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Database logging failed: ${e.message}")
        }
    }

    private fun getCuratedBackupProfiles(userGenre: String): List<DiscoverProfile> {
        val backup = listOf(
            DiscoverProfile(
                id = "p1",
                name = "Melis Kaya",
                age = 22,
                bio = "Alternatif rock bağımlısıyım. Mor ve Ötesi konserlerinde kaybolmayı severim. Akustik gitar çalıyorum.",
                avatarUrl = "",
                favoriteGenre = "Rock",
                topArtists = "Mor ve Ötesi, Duman, Arctic Monkeys, Pink Floyd",
                topTracks = "Cambaz, R U Mine?, Comfortably Numb",
                signatureSongId = "spotify:track:596133",
                signatureSongTitle = "Cambaz",
                signatureSongArtist = "Mor ve Ötesi",
                compatibilityPercentage = if (userGenre.lowercase() == "rock") 95 else 72
            ),
            DiscoverProfile(
                id = "p2",
                name = "Burak Yılmaz",
                age = 25,
                bio = "Analog synthesizer'lar ve vintage drum machine hayranıyım. Gece sürüşlerinde synthwave dinlemeyi çok severim.",
                avatarUrl = "",
                favoriteGenre = "Electronic",
                topArtists = "Daft Punk, Kavinsky, Moderat, Kraftwerk",
                topTracks = "Nightcall, Get Lucky, Direct Source",
                signatureSongId = "spotify:track:596137",
                signatureSongTitle = "Nightcall",
                signatureSongArtist = "Kavinsky",
                compatibilityPercentage = if (userGenre.lowercase() == "electronic") 97 else 64
            ),
            DiscoverProfile(
                id = "p3",
                name = "Ceren Şahin",
                age = 24,
                bio = "Caz barlarında vakit geçirmek hayat felsefem. Yağmurlu günlerde Miles Davis plakları dinlerim.",
                avatarUrl = "",
                favoriteGenre = "Jazz",
                topArtists = "Miles Davis, Billie Holiday, Norah Jones, Chet Baker",
                topTracks = "Blue in Green, Come Away With Me, Autumn Leaves",
                signatureSongId = "spotify:track:596138",
                signatureSongTitle = "Blue in Green",
                signatureSongArtist = "Miles Davis",
                compatibilityPercentage = 80
            ),
            DiscoverProfile(
                id = "p4",
                name = "Kaan Demir",
                age = 27,
                bio = "Hip-hop beatleri yapıyorum. Eski okul rap kültürüne hayranım. Türkçe rap ve underground sahnesini takip ediyorum.",
                avatarUrl = "",
                favoriteGenre = "Hip-Hop",
                topArtists = "Ceza, Sagopa Kajmer, Eminem, Kendrick Lamar",
                topTracks = "Neyim Var Ki, Lose Yourself, HUMBLE.",
                signatureSongId = "spotify:track:596139",
                signatureSongTitle = "Neyim Var Ki",
                signatureSongArtist = "Ceza ft. Sagopa",
                compatibilityPercentage = if (userGenre.lowercase() == "hip-hop") 94 else 58
            ),
            DiscoverProfile(
                id = "p5",
                name = "İrem Öztürk",
                age = 23,
                bio = "Indie pop melodileri ve melankolik şarkılar vazgeçilmezim. Kendi halinde şarkı sözleri yazıyorum.",
                avatarUrl = "",
                favoriteGenre = "Pop",
                topArtists = "Sertab Erener, Sezen Aksu, Lana Del Rey, Billie Eilish",
                topTracks = "Rengarenk, Video Games, Bad Guy",
                signatureSongId = "spotify:track:596131",
                signatureSongTitle = "Video Games",
                signatureSongArtist = "Lana Del Rey",
                compatibilityPercentage = if (userGenre.lowercase() == "pop") 91 else 76
            ),
            DiscoverProfile(
                id = "p6",
                name = "Tolga Arslan",
                age = 29,
                bio = "Klasik müzik piyanistiyim. Bach ve Chopin melodileriyle ruhumu dinlendiririm. Konserler veriyorum.",
                avatarUrl = "",
                favoriteGenre = "Classical",
                topArtists = "Bach, Chopin, Beethoven, Mozart",
                topTracks = "Nocturne Op. 9 No. 2, Moonlight Sonata",
                signatureSongId = "spotify:track:596144",
                signatureSongTitle = "Nocturne Op. 9 No. 2",
                signatureSongArtist = "Chopin",
                compatibilityPercentage = if (userGenre.lowercase() == "classical") 98 else 50
            )
        )
        return backup
    }
}
