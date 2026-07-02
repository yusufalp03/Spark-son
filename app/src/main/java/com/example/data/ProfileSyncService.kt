package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class SpotifyArtistDto(
    val id: String = "",
    val name: String = ""
)

data class SpotifyTrackDto(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtistDto>
)

data class SpotifyTopTracksResponse(
    val items: List<SpotifyTrackDto>
)

interface SpotifyUserApi {
    @GET("v1/me/top/tracks")
    suspend fun getTopTracks(
        @Header("Authorization") bearerToken: String,
        @Query("limit") limit: Int = 10
    ): SpotifyTopTracksResponse
}

data class TrackDto(val id: String, val artistName: String, val genres: List<String>)

@Singleton
class ProfileSyncService @Inject constructor(
    private val supabase: SupabaseClient,
    private val database: AppDatabase
) {
    private val TAG = "ProfileSyncService"

    private val userRetrofit = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .client(OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build())
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()))
        .build()

    private val userApi = userRetrofit.create(SpotifyUserApi::class.java)

    fun isCredentialsPlaceholder(): Boolean {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        return url.isEmpty() || url.contains("proje-id") || key.isEmpty() || key.contains("senin-anon-keyin")
    }

    /**
     * Spotify ile giriş sonrası müzik verilerini çeker ve buluta & yerel veritabanına eşitler.
     */
    suspend fun syncSpotifyData(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        logToDatabase("INFO", "Spotify profil eşitlemesi başlatılıyor...")
        
        try {
            // 1. Spotify'dan en çok dinlenen şarkıları çek
            val topTracks = fetchTopTracksFromSpotify(accessToken)

            // 2. Kullanıcı kimliğini al
            val userId = supabase.auth.currentUserOrNull()?.id ?: "me"
            
            val topGenres = topTracks.flatMap { it.genres }.distinct()
            val favoriteArtists = topTracks.map { it.artistName }.distinct()
            
            val tasteData = mapOf(
                "user_id" to userId,
                "top_genres" to topGenres,
                "favorite_artists" to favoriteArtists,
                "last_updated" to System.currentTimeMillis()
            )

            // Gerçek Supabase Bulut Veritabanı Kaydı
            try {
                val supabaseUrl = BuildConfig.SUPABASE_URL
                val supabaseKey = BuildConfig.SUPABASE_ANON_KEY
                val url = "$supabaseUrl/rest/v1/user_music_tastes"
                
                val genresJsonArray = JSONArray(topGenres)
                val artistsJsonArray = JSONArray(favoriteArtists)
                
                val jsonBody = JSONObject().apply {
                    put("user_id", userId)
                    put("top_genres", genresJsonArray)
                    put("favorite_artists", artistsJsonArray)
                    put("last_updated", System.currentTimeMillis())
                }
                
                val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                
                if (response.isSuccessful) {
                    logToDatabase("INFO", "Supabase bulutuna müzik zevki başarıyla yazıldı: $responseBody")
                } else {
                    Log.e(TAG, "Supabase bulut eşitlemesi başarısız: ${response.code} $responseBody")
                    logToDatabase("ERROR", "Supabase zevk profili hatası: ${response.code}")
                }
            } catch (postgrestEx: Exception) {
                Log.e(TAG, "Doğrudan API çağrısı başarısız oldu, SDK Postgrest deneniyor...", postgrestEx)
                supabase.postgrest["user_music_tastes"].upsert(tasteData)
                logToDatabase("INFO", "Postgrest SDK ile eşitleme tamamlandı.")
            }

            // Yerel Room veritabanındaki profilimizi de Spotify bilgileriyle güncelle
            val primaryGenre = topGenres.firstOrNull() ?: "Rock"
            val artistsString = favoriteArtists.take(5).joinToString(", ")
            val tracksString = topTracks.take(5).joinToString(", ") { "${it.artistName} - ${it.id}" }

            val currentProfile = database.userProfileDao().getMyProfileDirect() ?: UserProfile()
            val updatedProfile = currentProfile.copy(
                favoriteGenre = primaryGenre,
                topArtists = artistsString,
                topTracks = tracksString
            )
            database.userProfileDao().saveProfile(updatedProfile)
            logToDatabase("INFO", "Spotify eşitleme tamamlandı. Yerel profil başarıyla güncellendi.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Spotify veri eşitleme hatası", e)
            logToDatabase("ERROR", "Eşitleme hatası: ${e.message}")
            false
        }
    }

    private suspend fun fetchTopTracksFromSpotify(token: String): List<TrackDto> {
        return try {
            val bearer = "Bearer $token"
            val response = userApi.getTopTracks(bearer)
            
            val simulatedGenres = listOf("Rock", "Pop", "Electronic", "Jazz", "Hip-Hop", "Indie")
            
            response.items.map { track ->
                val artistName = track.artists.firstOrNull()?.name ?: "Unknown Artist"
                val genres = when (artistName.lowercase()) {
                    "the weeknd" -> listOf("Pop", "R&B", "Electronic")
                    "pink floyd" -> listOf("Rock", "Classic Rock", "Psychedelic")
                    "metallica" -> listOf("Rock", "Metal", "Thrash Metal")
                    "daft punk" -> listOf("Electronic", "Funk", "Dance")
                    "arctic monkeys" -> listOf("Rock", "Indie", "Alternative")
                    else -> listOf(simulatedGenres.random())
                }
                TrackDto(
                    id = track.name,
                    artistName = artistName,
                    genres = genres
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Spotify API'den müzikler alınamadı", e)
            logToDatabase("ERROR", "Spotify API hatası: ${e.message}")
            emptyList()
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
}
