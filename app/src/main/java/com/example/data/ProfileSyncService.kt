package com.example.data

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.Locale

data class SpotifyArtistDto(
    val id: String = "",
    val name: String = "",
    val genres: List<String> = emptyList()
)

data class SpotifyTopArtistsResponse(
    val items: List<SpotifyArtistDto>
)

data class SpotifyTrackArtistDto(
    val name: String = ""
)

data class SpotifyUserTrackDto(
    val id: String,
    val name: String,
    val artists: List<SpotifyTrackArtistDto>
)

data class SpotifyTopTracksResponse(
    val items: List<SpotifyUserTrackDto>
)

interface SpotifyUserApi {
    @GET("v1/me/top/artists")
    suspend fun getTopArtists(
        @Header("Authorization") bearerToken: String,
        @Query("limit") limit: Int = 10
    ): SpotifyTopArtistsResponse

    @GET("v1/me/top/tracks")
    suspend fun getTopTracks(
        @Header("Authorization") bearerToken: String,
        @Query("limit") limit: Int = 10
    ): SpotifyTopTracksResponse
}

/**
 * Spotify OAuth girişinden sonra kullanıcının gerçek müzik verilerini
 * (en çok dinlediği sanatçılar, türler, şarkılar) çekip hem yerel Room
 * profiline hem de Supabase'deki profiles tablosuna yazar.
 */
class ProfileSyncService(
    private val supabaseService: SupabaseService,
    private val database: AppDatabase
) {
    private val TAG = "ProfileSyncService"

    private val userRetrofit = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .client(SharedHttp.client)
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()))
        .build()

    private val userApi = userRetrofit.create(SpotifyUserApi::class.java)

    /**
     * Spotify verilerini eşitler. [spotifyAccessToken] Supabase oturumundaki
     * providerToken olmalıdır (Supabase JWT'si DEĞİL - Spotify API onu kabul etmez).
     * Token yoksa (ör. eski oturum geri yüklendiyse) sessizce atlanır.
     */
    suspend fun syncSpotifyData(spotifyAccessToken: String?): Boolean = withContext(Dispatchers.IO) {
        if (spotifyAccessToken.isNullOrBlank()) {
            Log.i(TAG, "Spotify provider token yok; profil eşitlemesi atlandı.")
            return@withContext false
        }

        try {
            val bearer = "Bearer $spotifyAccessToken"

            val topArtists = try {
                userApi.getTopArtists(bearer).items
            } catch (e: Exception) {
                Log.e(TAG, "Spotify top artists alınamadı", e)
                emptyList()
            }

            val topTracks = try {
                userApi.getTopTracks(bearer).items
            } catch (e: Exception) {
                Log.e(TAG, "Spotify top tracks alınamadı", e)
                emptyList()
            }

            if (topArtists.isEmpty() && topTracks.isEmpty()) {
                return@withContext false
            }

            // Gerçek tür bilgisi sanatçı nesnelerinden gelir
            val genreCounts = topArtists
                .flatMap { it.genres }
                .groupingBy { it.lowercase(Locale.ROOT) }
                .eachCount()
            val primaryGenre = genreCounts.maxByOrNull { it.value }?.key
                ?.replaceFirstChar { it.titlecase(Locale.ROOT) }
                ?: ""

            val artistsString = topArtists.take(5).joinToString(", ") { it.name }
            val tracksString = topTracks.take(5).joinToString(", ") { track ->
                val artistName = track.artists.firstOrNull()?.name
                if (artistName != null) "${track.name} - $artistName" else track.name
            }

            // Supabase kullanıcı metadata'sından isim/avatar (ilk girişte profil boş olur)
            val userInfo = supabaseService.supabaseClient.auth.currentUserOrNull()
            val metaName = userInfo?.userMetadata?.get("name")?.toString()?.trim('"') ?: ""
            val metaAvatar = userInfo?.userMetadata?.get("avatar_url")?.toString()?.trim('"') ?: ""

            val currentProfile = database.userProfileDao().getMyProfileDirect() ?: UserProfile()
            val updatedProfile = currentProfile.copy(
                name = currentProfile.name.ifBlank { metaName },
                avatarUrl = currentProfile.avatarUrl.ifBlank { metaAvatar },
                favoriteGenre = if (primaryGenre.isNotBlank()) primaryGenre else currentProfile.favoriteGenre,
                topArtists = if (artistsString.isNotBlank()) artistsString else currentProfile.topArtists,
                topTracks = if (tracksString.isNotBlank()) tracksString else currentProfile.topTracks
            )
            database.userProfileDao().saveProfile(updatedProfile)

            // Buluttaki profili de güncelle ki diğer kullanıcılar keşfette görsün
            supabaseService.upsertMyProfile(updatedProfile)

            Log.i(TAG, "Spotify eşitleme tamamlandı: tür=$primaryGenre, ${topArtists.size} sanatçı")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Spotify veri eşitleme hatası", e)
            false
        }
    }
}
