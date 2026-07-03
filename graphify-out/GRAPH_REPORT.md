# Graph Report - Spark-son  (2026-07-03)

## Corpus Check
- 30 files · ~18,368 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 317 nodes · 499 edges · 23 communities (20 shown, 3 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 10 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `0143c248`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Room Database & DAOs|Room Database & DAOs]]
- [[_COMMUNITY_ViewModel & Profile State|ViewModel & Profile State]]
- [[_COMMUNITY_MainActivity & Profile UI|MainActivity & Profile UI]]
- [[_COMMUNITY_Compose Screens (ChatAdmin)|Compose Screens (Chat/Admin)]]
- [[_COMMUNITY_Chat & Swipe Repository Logic|Chat & Swipe Repository Logic]]
- [[_COMMUNITY_Supabase Service & Secure Prefs|Supabase Service & Secure Prefs]]
- [[_COMMUNITY_Auth & Login Flow|Auth & Login Flow]]
- [[_COMMUNITY_Spotify API Integration|Spotify API Integration]]
- [[_COMMUNITY_Session Management & Tests|Session Management & Tests]]
- [[_COMMUNITY_Spotify Profile Sync Service|Spotify Profile Sync Service]]
- [[_COMMUNITY_Build & Setup Docs|Build & Setup Docs]]
- [[_COMMUNITY_Instrumented Tests|Instrumented Tests]]
- [[_COMMUNITY_Unit Tests|Unit Tests]]
- [[_COMMUNITY_Theme Colors|Theme Colors]]
- [[_COMMUNITY_Typography|Typography]]
- [[_COMMUNITY_Root Build Script|Root Build Script]]
- [[_COMMUNITY_Gradle Settings|Gradle Settings]]
- [[_COMMUNITY_2) Findings (Prioritized)|2) Findings (Prioritized)]]

## God Nodes (most connected - your core abstractions)
1. `SparkViewModel` - 41 edges
2. `AppRepository` - 20 edges
3. `SupabaseService` - 18 edges
4. `UserProfile` - 13 edges
5. `ChatMessage` - 12 edges
6. `2) Findings (Prioritized)` - 12 edges
7. `DiscoverProfile` - 11 edges
8. `Match` - 11 edges
9. `AppDatabase` - 9 edges
10. `MainActivity` - 8 edges

## Surprising Connections (you probably didn't know these)
- `MainScaffold()` --calls--> `ChatTab()`  [INFERRED]
  app/src/main/java/com/example/MainActivity.kt → app/src/main/java/com/example/ui/screens/ChatTab.kt
- `MainScaffold()` --calls--> `DiscoverTab()`  [INFERRED]
  app/src/main/java/com/example/MainActivity.kt → app/src/main/java/com/example/ui/screens/DiscoverTab.kt
- `MainActivity` --references--> `SparkViewModel`  [EXTRACTED]
  app/src/main/java/com/example/MainActivity.kt → app/src/main/java/com/example/ui/SparkViewModel.kt
- `MainScaffold()` --calls--> `ProfileTab()`  [INFERRED]
  app/src/main/java/com/example/MainActivity.kt → app/src/main/java/com/example/ui/screens/ProfileTab.kt
- `MainScaffold()` --calls--> `SettingsTab()`  [INFERRED]
  app/src/main/java/com/example/MainActivity.kt → app/src/main/java/com/example/ui/screens/SettingsTab.kt

## Import Cycles
- None detected.

## Communities (23 total, 3 thin omitted)

### Community 0 - "Room Database & DAOs"
Cohesion: 0.10
Nodes (20): android, AndroidCodeVerifierCache, createEncryptedPrefs(), Boolean, Context, Flow, Int, List (+12 more)

### Community 1 - "ViewModel & Profile State"
Cohesion: 0.07
Nodes (28): 1) Optimization Summary, 1. Unified Clean Constructor-Injected Container (`SparkApp.kt`), 2. Database Indexing Patch (`Models.kt`), 2) Findings (Prioritized), 3. Localization Query Patch (`AppDatabase.kt` / `AppRepository.kt`), 3) Quick Wins (Do First), 4) Deeper Optimizations (Do Next), 4. Secure EncryptedSharedPreferences Storage Patch (`SupabaseService.kt`) (+20 more)

### Community 2 - "MainActivity & Profile UI"
Cohesion: 0.11
Nodes (10): AndroidViewModel, AuthUiState, Float, Flow, Int, Intent, List, String (+2 more)

### Community 3 - "Compose Screens (Chat/Admin)"
Cohesion: 0.11
Nodes (15): Intent, MainActivity, MainScaffold(), SplashScreen(), Modifier, String, ProfileInfoRow(), ProfileTab() (+7 more)

### Community 4 - "Chat & Swipe Repository Logic"
Cohesion: 0.13
Nodes (15): Boolean, Int, List, Long, String, SpotifyAlbumItem, SpotifyArtistItem, SpotifyAuthApi (+7 more)

### Community 5 - "Supabase Service & Secure Prefs"
Cohesion: 0.13
Nodes (8): AndroidSessionManager, UserSession, DummyCodeVerifierCache, DummySessionManager, ExampleRobolectricTest, String, UserSession, SessionManager

### Community 6 - "Auth & Login Flow"
Cohesion: 0.16
Nodes (7): AppRepository, Boolean, Flow, Int, Job, List, String

### Community 7 - "Spotify API Integration"
Cohesion: 0.19
Nodes (11): DiscoverProfileDao, List, DiscoverProfile, DiscoverCard(), DiscoverTab(), Boolean, Modifier, String (+3 more)

### Community 8 - "Session Management & Tests"
Cohesion: 0.11
Nodes (10): AppDatabase, getDatabase(), Context, Flow, UserFeedbackDao, UserProfileDao, SpotifyTrack, UserFeedback (+2 more)

### Community 9 - "Spotify Profile Sync Service"
Cohesion: 0.23
Nodes (5): ChatMessageDao, Converters, String, ChatMessage, SyncStatus

### Community 11 - "Build & Setup Docs"
Cohesion: 0.22
Nodes (10): Boolean, Int, String, ProfileSyncService, SpotifyArtistDto, SpotifyTopArtistsResponse, SpotifyTopTracksResponse, SpotifyTrackArtistDto (+2 more)

### Community 12 - "Instrumented Tests"
Cohesion: 0.26
Nodes (6): MatchDao, Match, ChatTab(), Modifier, LiveChatView(), MatchItemRow()

### Community 13 - "Unit Tests"
Cohesion: 0.25
Nodes (4): AuthService, Boolean, LoginScreen(), UserInfo

### Community 14 - "Theme Colors"
Cohesion: 0.22
Nodes (8): 1) Gereksinimler, 2) Supabase kurulumu, 3) Spotify Developer kurulumu, 4) Uygulamayı derleme, 5) Yayın (Release) derlemesi, Giriş (login) sorun giderme, Mimari özet, Spark 🎵⚡

### Community 15 - "Typography"
Cohesion: 0.17
Nodes (9): Float, Job, String, PreviewAudioPlayer, Job, String, ProfileSynthEngine, AudioTrack (+1 more)

## Knowledge Gaps
- **39 isolated node(s):** `SpotifyArtistDto`, `SpotifyTrackArtistDto`, `SpotifyUserTrackDto`, `SpotifyTracksList`, `SpotifyTrackItem` (+34 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SparkViewModel` connect `MainActivity & Profile UI` to `Compose Screens (Chat/Admin)`, `Supabase Service & Secure Prefs`, `Spotify API Integration`, `Session Management & Tests`, `Instrumented Tests`?**
  _High betweenness centrality (0.298) - this node is a cross-community bridge._
- **Why does `UserProfile` connect `Session Management & Tests` to `Room Database & DAOs`, `MainActivity & Profile UI`, `Build & Setup Docs`, `Auth & Login Flow`?**
  _High betweenness centrality (0.143) - this node is a cross-community bridge._
- **Why does `SpotifyTrack` connect `Session Management & Tests` to `Chat & Swipe Repository Logic`?**
  _High betweenness centrality (0.096) - this node is a cross-community bridge._
- **What connects `SpotifyArtistDto`, `SpotifyTrackArtistDto`, `SpotifyUserTrackDto` to the rest of the system?**
  _39 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Room Database & DAOs` be split into smaller, more focused modules?**
  _Cohesion score 0.09957325746799431 - nodes in this community are weakly interconnected._
- **Should `ViewModel & Profile State` be split into smaller, more focused modules?**
  _Cohesion score 0.06896551724137931 - nodes in this community are weakly interconnected._
- **Should `MainActivity & Profile UI` be split into smaller, more focused modules?**
  _Cohesion score 0.11088709677419355 - nodes in this community are weakly interconnected._