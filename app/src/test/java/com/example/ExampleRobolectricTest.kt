package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.SparkViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.CodeVerifierCache

class DummySessionManager : SessionManager {
    private var session: UserSession? = null

    override suspend fun saveSession(session: UserSession) {
        this.session = session
    }

    override suspend fun loadSession(): UserSession? {
        return session
    }

    override suspend fun deleteSession() {
        this.session = null
    }
}

class DummyCodeVerifierCache : CodeVerifierCache {
    private var codeVerifier: String? = null

    override suspend fun saveCodeVerifier(codeVerifier: String) {
        this.codeVerifier = codeVerifier
    }

    override suspend fun loadCodeVerifier(): String? {
        return codeVerifier
    }

    override suspend fun deleteCodeVerifier() {
        this.codeVerifier = null
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Spark", appName)
  }

  @Test
  fun `test custom sessionManager config`() {
    val client = createSupabaseClient("https://example.supabase.co", "key") {
        install(Auth) {
            sessionManager = DummySessionManager()
            codeVerifierCache = DummyCodeVerifierCache()
        }
    }
    assertNotNull(client)
  }

  @Test
  fun `instantiate SparkViewModel and verify not null`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = SparkViewModel(app)
    assertNotNull(viewModel)
  }
}





