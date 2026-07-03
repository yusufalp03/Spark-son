package com.example.data

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

// --- API Response Classes ---

data class SpotifyTokenResponse(
    @field:Json(name = "access_token") val accessToken: String,
    @field:Json(name = "token_type") val tokenType: String,
    @field:Json(name = "expires_in") val expiresIn: Int
)

data class SpotifySearchResponse(
    val tracks: SpotifyTracksList
)

data class SpotifyTracksList(
    val items: List<SpotifyTrackItem>
)

data class SpotifyTrackItem(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtistItem>,
    val album: SpotifyAlbumItem,
    @field:Json(name = "duration_ms") val durationMs: Long?
)

data class SpotifyArtistItem(
    val name: String
)

data class SpotifyAlbumItem(
    val images: List<SpotifyImageItem>
)

data class SpotifyImageItem(
    val url: String,
    val height: Int,
    val width: Int
)

// --- Retrofit Interfaces ---

interface SpotifyAuthApi {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun getAccessToken(
        @Header("Authorization") basicAuth: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): SpotifyTokenResponse
}

interface SpotifySearchApi {
    @GET("v1/search")
    suspend fun searchTracks(
        @Header("Authorization") bearerToken: String,
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 10
    ): SpotifySearchResponse
}

// --- Spotify Manager Service ---

/**
 * İmza şarkısı araması için Spotify Client Credentials akışı.
 * Token süresi takip edilir; süre dolmadan 60 sn önce yenilenir.
 */
class SpotifyService {
    private val TAG = "SpotifyService"

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val authRetrofit = Retrofit.Builder()
        .baseUrl("https://accounts.spotify.com/")
        .client(SharedHttp.client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val searchRetrofit = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .client(SharedHttp.client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val authApi = authRetrofit.create(SpotifyAuthApi::class.java)
    private val searchApi = searchRetrofit.create(SpotifySearchApi::class.java)

    private var cachedToken: String? = null
    private var tokenExpiresAt: Long = 0L

    fun isConfigured(): Boolean {
        val clientId = BuildConfig.SPOTIFY_CLIENT_ID
        val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET
        return clientId.isNotEmpty() && !clientId.contains("MY_SPOTIFY_CLIENT_ID") &&
            clientSecret.isNotEmpty() && !clientSecret.contains("MY_SPOTIFY_CLIENT_SECRET")
    }

    private suspend fun getAuthToken(): String? {
        if (!isConfigured()) {
            Log.w(TAG, "Spotify API anahtarları eksik; arama devre dışı.")
            return null
        }

        val token = cachedToken
        if (token != null && System.currentTimeMillis() < tokenExpiresAt) {
            return token
        }

        return try {
            val basicAuth = "Basic " + Base64.encodeToString(
                "${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}".toByteArray(),
                Base64.NO_WRAP
            )
            val response = authApi.getAccessToken(basicAuth)
            cachedToken = response.accessToken
            // 60 sn güvenlik payıyla yenile
            tokenExpiresAt = System.currentTimeMillis() + (response.expiresIn - 60) * 1000L
            cachedToken
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Spotify access token", e)
            cachedToken = null
            tokenExpiresAt = 0L
            null
        }
    }

    suspend fun searchTracks(query: String): List<SpotifyTrack> {
        val token = getAuthToken() ?: return emptyList()

        return try {
            val response = searchApi.searchTracks("Bearer $token", query)
            response.tracks.items.map { item ->
                SpotifyTrack(
                    id = item.id,
                    name = item.name,
                    artist = item.artists.firstOrNull()?.name ?: "Unknown Artist",
                    albumImageUrl = item.album.images.firstOrNull()?.url ?: "",
                    durationMs = item.durationMs ?: 0L
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Spotify search failed", e)
            emptyList()
        }
    }
}
