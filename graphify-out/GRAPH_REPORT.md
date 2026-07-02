# Graph Report - Spark-son  (2026-07-02)

## Corpus Check
- 29 files · ~17,472 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 321 nodes · 486 edges · 38 communities (21 shown, 17 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 15 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `0f3def0e`
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
- [[_COMMUNITY_FM Synth Audio Engine|FM Synth Audio Engine]]
- [[_COMMUNITY_Build & Setup Docs|Build & Setup Docs]]
- [[_COMMUNITY_Instrumented Tests|Instrumented Tests]]
- [[_COMMUNITY_Unit Tests|Unit Tests]]
- [[_COMMUNITY_2) Findings (Prioritized)|2) Findings (Prioritized)]]
- [[_COMMUNITY_AppDatabase|AppDatabase]]
- [[_COMMUNITY_UserProfile|UserProfile]]
- [[_COMMUNITY_Match|Match]]
- [[_COMMUNITY_CLAUDE|CLAUDE.md]]
- [[_COMMUNITY_Greeting Test Screenshot (corrupt PNG header)|Greeting Test Screenshot (corrupt PNG header)]]
- [[_COMMUNITY_AppContainer Dependency Injection|AppContainer Dependency Injection]]
- [[_COMMUNITY_ChatSync Pipeline Consolidation|Chat/Sync Pipeline Consolidation]]
- [[_COMMUNITY_Dagger Hilt|Dagger Hilt]]
- [[_COMMUNITY_Database Logging Overhead|Database Logging Overhead]]
- [[_COMMUNITY_EncryptedSharedPreferences|EncryptedSharedPreferences]]
- [[_COMMUNITY_Gemini API Key Setup|Gemini API Key Setup]]
- [[_COMMUNITY_Main-Thread Blocking in Auth Flows|Main-Thread Blocking in Auth Flows]]
- [[_COMMUNITY_MediaPlayer Release Leak|MediaPlayer Release Leak]]
- [[_COMMUNITY_Offline-First Room SSOT|Offline-First Room SSOT]]
- [[_COMMUNITY_Room Database Indexing|Room Database Indexing]]
- [[_COMMUNITY_Shared OkHttpClient Singleton|Shared OkHttpClient Singleton]]
- [[_COMMUNITY_Spotify Token Expiration Handling|Spotify Token Expiration Handling]]
- [[_COMMUNITY_Structured Concurrency|Structured Concurrency]]

## God Nodes (most connected - your core abstractions)
1. `SparkViewModel` - 39 edges
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

## Hyperedges (group relationships)
- **Offline-First Chat Sync Flow** — app_src_main_java_com_example_data_apprepository, app_src_main_java_com_example_data_appdatabase, app_src_main_java_com_example_data_models, app_src_main_java_com_example_ui_screens_chattab, app_src_main_java_com_example_ui_sparkviewmodel [INFERRED 0.85]
- **Optimization Audit Target Files** — app_src_main_java_com_example_ui_sparkviewmodel, app_src_main_java_com_example_data_apprepository, app_src_main_java_com_example_data_spotifyservice, app_src_main_java_com_example_data_supabaseservice, app_src_main_java_com_example_data_authservice, app_src_main_java_com_example_ui_screens_loginscreen [EXTRACTED 1.00]

## Communities (38 total, 17 thin omitted)

### Community 0 - "Room Database & DAOs"
Cohesion: 0.23
Nodes (5): ChatMessageDao, Converters, String, ChatMessage, SyncStatus

### Community 1 - "ViewModel & Profile State"
Cohesion: 0.12
Nodes (9): AndroidViewModel, AuthUiState, Flow, Int, Intent, List, String, SparkViewModel (+1 more)

### Community 2 - "MainActivity & Profile UI"
Cohesion: 0.10
Nodes (16): Intent, MainActivity, MainScaffold(), SplashScreen(), Modifier, String, ProfileInfoRow(), ProfileTab() (+8 more)

### Community 3 - "Compose Screens (Chat/Admin)"
Cohesion: 0.19
Nodes (11): DiscoverProfileDao, List, DiscoverProfile, DiscoverCard(), DiscoverTab(), Boolean, Modifier, String (+3 more)

### Community 4 - "Chat & Swipe Repository Logic"
Cohesion: 0.15
Nodes (8): AppRepository, Boolean, Flow, Int, Job, List, String, UserFeedback

### Community 5 - "Supabase Service & Secure Prefs"
Cohesion: 0.11
Nodes (20): android, AndroidCodeVerifierCache, createEncryptedPrefs(), Boolean, Context, Flow, Int, List (+12 more)

### Community 6 - "Auth & Login Flow"
Cohesion: 0.25
Nodes (4): AuthService, Boolean, LoginScreen(), UserInfo

### Community 7 - "Spotify API Integration"
Cohesion: 0.13
Nodes (15): Boolean, Int, List, Long, String, SpotifyAlbumItem, SpotifyArtistItem, SpotifyAuthApi (+7 more)

### Community 8 - "Session Management & Tests"
Cohesion: 0.13
Nodes (8): AndroidSessionManager, UserSession, DummyCodeVerifierCache, DummySessionManager, ExampleRobolectricTest, String, UserSession, SessionManager

### Community 9 - "Spotify Profile Sync Service"
Cohesion: 0.22
Nodes (10): Boolean, Int, String, ProfileSyncService, SpotifyArtistDto, SpotifyTopArtistsResponse, SpotifyTopTracksResponse, SpotifyTrackArtistDto (+2 more)

### Community 10 - "FM Synth Audio Engine"
Cohesion: 0.39
Nodes (4): Job, String, ProfileSynthEngine, AudioTrack

### Community 11 - "Build & Setup Docs"
Cohesion: 0.22
Nodes (8): 1) Gereksinimler, 2) Supabase kurulumu, 3) Spotify Developer kurulumu, 4) Uygulamayı derleme, 5) Yayın (Release) derlemesi, Giriş (login) sorun giderme, Mimari özet, Spark 🎵⚡

### Community 18 - "2) Findings (Prioritized)"
Cohesion: 0.07
Nodes (28): 1) Optimization Summary, 1. Unified Clean Constructor-Injected Container (`SparkApp.kt`), 2. Database Indexing Patch (`Models.kt`), 2) Findings (Prioritized), 3. Localization Query Patch (`AppDatabase.kt` / `AppRepository.kt`), 3) Quick Wins (Do First), 4) Deeper Optimizations (Do Next), 4. Secure EncryptedSharedPreferences Storage Patch (`SupabaseService.kt`) (+20 more)

### Community 19 - "AppDatabase"
Cohesion: 0.23
Nodes (5): AppDatabase, getDatabase(), Context, UserFeedbackDao, RoomDatabase

### Community 20 - "UserProfile"
Cohesion: 0.18
Nodes (5): Flow, UserProfileDao, SpotifyTrack, UserProfile, Float

### Community 21 - "Match"
Cohesion: 0.26
Nodes (6): MatchDao, Match, ChatTab(), Modifier, LiveChatView(), MatchItemRow()

## Knowledge Gaps
- **52 isolated node(s):** `SpotifyArtistDto`, `SpotifyTrackArtistDto`, `SpotifyUserTrackDto`, `SpotifyTracksList`, `SpotifyTrackItem` (+47 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **17 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SparkViewModel` connect `ViewModel & Profile State` to `MainActivity & Profile UI`, `Compose Screens (Chat/Admin)`, `Session Management & Tests`, `UserProfile`, `Match`?**
  _High betweenness centrality (0.286) - this node is a cross-community bridge._
- **Why does `UserProfile` connect `UserProfile` to `Spotify Profile Sync Service`, `Chat & Swipe Repository Logic`, `Supabase Service & Secure Prefs`, `ViewModel & Profile State`?**
  _High betweenness centrality (0.140) - this node is a cross-community bridge._
- **Why does `SpotifyTrack` connect `UserProfile` to `Spotify API Integration`?**
  _High betweenness centrality (0.093) - this node is a cross-community bridge._
- **What connects `SpotifyArtistDto`, `SpotifyTrackArtistDto`, `SpotifyUserTrackDto` to the rest of the system?**
  _54 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ViewModel & Profile State` be split into smaller, more focused modules?**
  _Cohesion score 0.12169312169312169 - nodes in this community are weakly interconnected._
- **Should `MainActivity & Profile UI` be split into smaller, more focused modules?**
  _Cohesion score 0.10333333333333333 - nodes in this community are weakly interconnected._
- **Should `Supabase Service & Secure Prefs` be split into smaller, more focused modules?**
  _Cohesion score 0.10668563300142248 - nodes in this community are weakly interconnected._