package com.example.data

import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Spotify
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthService(private val supabase: SupabaseClient) {

    fun isCredentialsPlaceholder(): Boolean {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        return url.isEmpty() || url.contains("proje-id") || key.isEmpty() ||
            key.contains("SUPABASE_ANON_KEY") || key.contains("senin-anon-keyin")
    }

    val currentUser: UserInfo?
        get() {
            return try {
                supabase.auth.currentUserOrNull()
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Spotify ile OAuth (PKCE) akışını başlatır. Supabase SDK'den OAuth URL'ini
     * alır ve tarayıcı üzerinden yönlendirir; dönüş spark://login deep link'i
     * ile MainActivity'ye gelir.
     */
    suspend fun signInWithSpotify(onLaunchBrowser: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = supabase.auth.getOAuthUrl(provider = Spotify, redirectUrl = "spark://login") {
                // Müzik verilerini çekebilmek için gerekli Spotify izinleri
                scopes.add("user-read-email")
                scopes.add("user-top-read")
            }
            withContext(Dispatchers.Main) { onLaunchBrowser(url) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
