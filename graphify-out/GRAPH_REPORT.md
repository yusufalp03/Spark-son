# Graph Report - Spark-son  (2026-07-04)

## Corpus Check
- 30 files · ~19,892 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 330 nodes · 531 edges · 23 communities (20 shown, 3 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 13 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `5a4af91f`
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
- [[_COMMUNITY_ProfileTab|ProfileTab]]
- [[_COMMUNITY_Build & Setup Docs|Build & Setup Docs]]
- [[_COMMUNITY_Match|Match]]
- [[_COMMUNITY_Spark 🎵⚡|Spark 🎵⚡]]
- [[_COMMUNITY_Theme Colors|Theme Colors]]
- [[_COMMUNITY_Typography|Typography]]
- [[_COMMUNITY_Root Build Script|Root Build Script]]
- [[_COMMUNITY_Gradle Settings|Gradle Settings]]

## God Nodes (most connected - your core abstractions)
1. `SparkViewModel` - 41 edges
2. `AppRepository` - 20 edges
3. `SupabaseService` - 18 edges
4. `UserProfile` - 13 edges
5. `DiscoverProfile` - 12 edges
6. `ChatMessage` - 12 edges
7. `2) Findings (Prioritized)` - 12 edges
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
Cohesion: 0.12
Nodes (9): ChatMessageDao, Converters, Flow, String, UserProfileDao, ChatMessage, SpotifyTrack, SyncStatus (+1 more)

### Community 2 - "MainActivity & Profile UI"
Cohesion: 0.11
Nodes (11): AndroidViewModel, AuthUiState, Context, Float, Flow, Int, Intent, List (+3 more)

### Community 3 - "Compose Screens (Chat/Admin)"
Cohesion: 0.07
Nodes (28): 1) Optimization Summary, 1. Unified Clean Constructor-Injected Container (`SparkApp.kt`), 2. Database Indexing Patch (`Models.kt`), 2) Findings (Prioritized), 3. Localization Query Patch (`AppDatabase.kt` / `AppRepository.kt`), 3) Quick Wins (Do First), 4) Deeper Optimizations (Do Next), 4. Secure EncryptedSharedPreferences Storage Patch (`SupabaseService.kt`) (+20 more)

### Community 4 - "Chat & Swipe Repository Logic"
Cohesion: 0.10
Nodes (17): Intent, MainActivity, MainScaffold(), SplashScreen(), formatSeconds(), Float, Modifier, String (+9 more)

### Community 5 - "Supabase Service & Secure Prefs"
Cohesion: 0.13
Nodes (8): AndroidSessionManager, UserSession, DummyCodeVerifierCache, DummySessionManager, ExampleRobolectricTest, String, UserSession, SessionManager

### Community 6 - "Auth & Login Flow"
Cohesion: 0.13
Nodes (15): Boolean, Int, List, Long, String, SpotifyAlbumItem, SpotifyArtistItem, SpotifyAuthApi (+7 more)

### Community 7 - "Spotify API Integration"
Cohesion: 0.15
Nodes (8): AppRepository, Boolean, Flow, Int, Job, List, String, UserFeedback

### Community 8 - "Session Management & Tests"
Cohesion: 0.14
Nodes (12): Context, Float, Int, Job, String, SpotifyRemotePlayer, Job, String (+4 more)

### Community 9 - "Spotify Profile Sync Service"
Cohesion: 0.23
Nodes (5): AppDatabase, getDatabase(), Context, UserFeedbackDao, RoomDatabase

### Community 10 - "ProfileTab"
Cohesion: 0.22
Nodes (10): Boolean, Int, String, ProfileSyncService, SpotifyArtistDto, SpotifyTopArtistsResponse, SpotifyTopTracksResponse, SpotifyTrackArtistDto (+2 more)

### Community 11 - "Build & Setup Docs"
Cohesion: 0.15
Nodes (16): DiscoverProfileDao, List, DiscoverProfile, DiscoverCard(), DiscoverTab(), Boolean, Float, Modifier (+8 more)

### Community 12 - "Match"
Cohesion: 0.26
Nodes (6): MatchDao, Match, ChatTab(), Modifier, LiveChatView(), MatchItemRow()

### Community 13 - "Spark 🎵⚡"
Cohesion: 0.20
Nodes (9): 1) Gereksinimler, 2) Supabase kurulumu, 3) Spotify Developer kurulumu, 4) Uygulamayı derleme, 5) Yayın (Release) derlemesi, Giriş (login) sorun giderme, İmza şarkısı kesiti (App Remote), Mimari özet (+1 more)

### Community 14 - "Theme Colors"
Cohesion: 0.33
Nodes (3): AuthService, Boolean, UserInfo

## Knowledge Gaps
- **39 isolated node(s):** `SpotifyArtistDto`, `SpotifyTrackArtistDto`, `SpotifyUserTrackDto`, `SpotifyTracksList`, `SpotifyTrackItem` (+34 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SparkViewModel` connect `MainActivity & Profile UI` to `ViewModel & Profile State`, `Chat & Swipe Repository Logic`, `Supabase Service & Secure Prefs`, `Build & Setup Docs`, `Match`?**
  _High betweenness centrality (0.272) - this node is a cross-community bridge._
- **Why does `UserProfile` connect `ViewModel & Profile State` to `Room Database & DAOs`, `ProfileTab`, `MainActivity & Profile UI`, `Spotify API Integration`?**
  _High betweenness centrality (0.132) - this node is a cross-community bridge._
- **Why does `DiscoverProfile` connect `Build & Setup Docs` to `Room Database & DAOs`, `ViewModel & Profile State`, `MainActivity & Profile UI`, `Spotify API Integration`?**
  _High betweenness centrality (0.102) - this node is a cross-community bridge._
- **What connects `SpotifyArtistDto`, `SpotifyTrackArtistDto`, `SpotifyUserTrackDto` to the rest of the system?**
  _39 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Room Database & DAOs` be split into smaller, more focused modules?**
  _Cohesion score 0.09957325746799431 - nodes in this community are weakly interconnected._
- **Should `ViewModel & Profile State` be split into smaller, more focused modules?**
  _Cohesion score 0.11692307692307692 - nodes in this community are weakly interconnected._
- **Should `MainActivity & Profile UI` be split into smaller, more focused modules?**
  _Cohesion score 0.10984848484848485 - nodes in this community are weakly interconnected._