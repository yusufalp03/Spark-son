import java.net.URI
import java.util.zip.ZipInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.spark.musicdating"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Yayın anahtarı ortam değişkenleriyle verilir; yoksa release derlemesi
  // yerel test amaçlı debug imzasına düşer (Play Store bunu kabul etmez).
  val releaseKeystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
  val hasReleaseKeystore = file(releaseKeystorePath).exists()

  signingConfigs {
    if (hasReleaseKeystore) {
      create("release") {
        storeFile = file(releaseKeystorePath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = if (hasReleaseKeystore) {
        signingConfigs.getByName("release")
      } else {
        signingConfigs.getByName("debug")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Spotify App Remote SDK Maven'da yayınlanmaz; ilk derlemede GitHub releases'tan
// otomatik indirilir. GitHub'a erişilemeyen ortamlar için aynı AAR, repo
// ağacını zip olarak sunan Go module proxy'sinden çıkarılır. İkisi de
// başarısız olursa hata mesajındaki adresten elle indirip app/libs/ altına koyun.
val spotifyAppRemoteAar = file("libs/spotify-app-remote-release-0.8.0.aar")
val spotifyAppRemoteUrl =
  "https://github.com/spotify/android-sdk/releases/download/v0.8.0-appremote_v2.1.0-auth/spotify-app-remote-release-0.8.0.aar"
val spotifyAppRemoteFallbackZip =
  "https://proxy.golang.org/github.com/spotify/android-sdk/@v/v0.4.1-0.20230704105026-5aa4d62465f6.zip"

val downloadSpotifyAppRemote = tasks.register("downloadSpotifyAppRemote") {
  outputs.file(spotifyAppRemoteAar)
  onlyIf { !spotifyAppRemoteAar.exists() }
  doLast {
    spotifyAppRemoteAar.parentFile.mkdirs()
    val aarName = spotifyAppRemoteAar.name
    val direct = runCatching {
      URI(spotifyAppRemoteUrl).toURL().openStream().use { input ->
        spotifyAppRemoteAar.outputStream().use { output -> input.copyTo(output) }
      }
    }
    if (direct.isSuccess) return@doLast
    runCatching {
      URI(spotifyAppRemoteFallbackZip).toURL().openStream().use { input ->
        ZipInputStream(input).use { zip ->
          var entry = zip.nextEntry
          while (entry != null && !entry.name.endsWith("/app-remote-lib/$aarName")) {
            entry = zip.nextEntry
          }
          checkNotNull(entry) { "$aarName yedek zip içinde bulunamadı" }
          spotifyAppRemoteAar.outputStream().use { output -> zip.copyTo(output) }
        }
      }
    }.onFailure { e ->
      spotifyAppRemoteAar.delete()
      throw GradleException(
        "Spotify App Remote AAR indirilemedi. Elle indirip app/libs/ altına koyun: $spotifyAppRemoteUrl",
        e
      )
    }
  }
}
tasks.named("preBuild") { dependsOn(downloadSpotifyAppRemote) }

configurations.all {
  resolutionStrategy {
    force("io.ktor:ktor-client-core:2.3.11")
    force("io.ktor:ktor-client-android:2.3.11")
    force("io.ktor:ktor-client-content-negotiation:2.3.11")
    force("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
    force("io.ktor:ktor-client-logging:2.3.11")
    force("io.ktor:ktor-client-okhttp:2.3.11")
    force("io.ktor:ktor-client-websockets:2.3.11")
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)

  // Spotify App Remote: imza şarkısı kesitini cihazdaki Spotify uygulamasıyla çalar
  implementation(files("libs/spotify-app-remote-release-0.8.0.aar"))
  implementation("com.google.code.gson:gson:2.10.1")

  // Görseller: avatar ve albüm kapağı yükleme
  implementation("io.coil-kt:coil-compose:2.6.0")

  // Supabase
  implementation(libs.supabase.postgrest)
  implementation(libs.supabase.realtime)
  implementation(libs.supabase.gotrue)
  implementation(libs.ktor.client.android)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)
}
