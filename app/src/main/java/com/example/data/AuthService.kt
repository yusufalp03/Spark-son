package com.example.data

import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Spotify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

// Support the requested DI annotation patterns without compile errors
annotation class Inject
annotation class Singleton

@Singleton
class AuthService @Inject constructor(private val supabase: SupabaseClient) {

    private val _mockUserLoggedIn = MutableStateFlow(false)
    val mockUserLoggedIn: StateFlow<Boolean> = _mockUserLoggedIn.asStateFlow()

    fun isCredentialsPlaceholder(): Boolean {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        return url.isEmpty() || url.contains("proje-id") || key.isEmpty() || key.contains("senin-anon-keyin")
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
     * Spotify ile OAuth akışını başlatır.
     * Supabase SDK'den OAuth URL'ini alır ve browser üzerinden yönlendirir.
     */
    suspend fun signInWithSpotify(onLaunchBrowser: (String) -> Unit): Boolean = withContext(Dispatchers.Main) {
        try {
            val url = supabase.auth.getOAuthUrl(provider = Spotify, redirectUrl = "spark://login") {
                // Burada gerekli olan Spotify Scope'larını ekliyoruz (Müzik verilerini çekmek için)
                // Sizin yazdığınız CodeVerifier Cache arka planda PKCE kodlarını 
                // SharedPreferences üzerinden otomatik yönetecek.
                scopes.add("user-read-email")
                scopes.add("user-top-read")
            }
            onLaunchBrowser(url)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Demo modu ile giriş yapar (geliştirici veya yapılandırma eksikliği durumunda bypass sağlar).
     */
    suspend fun signInWithDemo(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Simulated network delay to prevent layout thrashing and give visual feedback
            delay(500)
            _mockUserLoggedIn.value = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun signOut() {
        _mockUserLoggedIn.value = false
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
