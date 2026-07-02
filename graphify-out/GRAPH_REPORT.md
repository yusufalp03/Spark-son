# Graph Report - /home/user/Spark-son  (2026-07-02)

## Corpus Check
- Corpus is ~18,210 words - fits in a single context window. You may not need a graph.

## Summary
- 276 nodes · 480 edges · 18 communities (15 shown, 3 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 23 edges (avg confidence: 0.81)
- Token cost: 21,000 input · 3,200 output

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

## God Nodes (most connected - your core abstractions)
1. `SparkViewModel` - 38 edges
2. `AppRepository` - 23 edges
3. `UserProfile` - 12 edges
4. `DiscoverProfile` - 12 edges
5. `ChatMessage` - 10 edges
6. `AppDatabase` - 9 edges
7. `AuthService` - 9 edges
8. `Match` - 9 edges
9. `SystemLog` - 9 edges
10. `MainActivity` - 8 edges

## Surprising Connections (you probably didn't know these)
- `MainActivity` --references--> `SparkViewModel`  [EXTRACTED]
  app/src/main/java/com/example/MainActivity.kt → app/src/main/java/com/example/ui/SparkViewModel.kt
- `AppRepository` --references--> `DiscoverProfile`  [EXTRACTED]
  app/src/main/java/com/example/data/AppRepository.kt → app/src/main/java/com/example/data/Models.kt
- `AppRepository` --references--> `UserProfile`  [EXTRACTED]
  app/src/main/java/com/example/data/AppRepository.kt → app/src/main/java/com/example/data/Models.kt
- `DiscoverCard()` --references--> `DiscoverProfile`  [EXTRACTED]
  app/src/main/java/com/example/ui/screens/DiscoverTab.kt → app/src/main/java/com/example/data/Models.kt
- `MatchCelebrationDialog()` --references--> `DiscoverProfile`  [EXTRACTED]
  app/src/main/java/com/example/ui/screens/DiscoverTab.kt → app/src/main/java/com/example/data/Models.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Offline-First Chat Sync Flow** — app_src_main_java_com_example_data_apprepository, app_src_main_java_com_example_data_appdatabase, app_src_main_java_com_example_data_models, app_src_main_java_com_example_ui_screens_chattab, app_src_main_java_com_example_ui_sparkviewmodel [INFERRED 0.85]
- **Optimization Audit Target Files** — app_src_main_java_com_example_ui_sparkviewmodel, app_src_main_java_com_example_data_apprepository, app_src_main_java_com_example_data_spotifyservice, app_src_main_java_com_example_data_supabaseservice, app_src_main_java_com_example_data_authservice, app_src_main_java_com_example_ui_screens_loginscreen [EXTRACTED 1.00]

## Communities (18 total, 3 thin omitted)

### Community 0 - "Room Database & DAOs"
Cohesion: 0.08
Nodes (17): AppDatabase, ChatMessageDao, Converters, DiscoverProfileDao, getDatabase(), Context, Flow, List (+9 more)

### Community 1 - "ViewModel & Profile State"
Cohesion: 0.09
Nodes (13): AndroidViewModel, UserProfileDao, UserProfile, Boolean, Flow, Int, Intent, List (+5 more)

### Community 2 - "MainActivity & Profile UI"
Cohesion: 0.09
Nodes (16): Intent, MainActivity, Modifier, String, ProfileInfoRow(), ProfileTab(), SignatureSongTrimmerDialog(), Modifier (+8 more)

### Community 3 - "Compose Screens (Chat/Admin)"
Cohesion: 0.13
Nodes (21): androidx, AdminPanel(), FeedbackRow(), Modifier, String, LogRow(), MetricCard(), ChatTab() (+13 more)

### Community 4 - "Chat & Swipe Repository Logic"
Cohesion: 0.20
Nodes (10): AppRepository, Boolean, Flow, Int, List, String, Match, SystemLog (+2 more)

### Community 5 - "Supabase Service & Secure Prefs"
Cohesion: 0.14
Nodes (13): android, AndroidCodeVerifierCache, createEncryptedPrefs(), Boolean, Context, List, String, SupabaseService (+5 more)

### Community 6 - "Auth & Login Flow"
Cohesion: 0.12
Nodes (14): AuthService, Inject, Boolean, StateFlow, Singleton, LoginScreen(), Chat/Sync Pipeline Consolidation, Database Logging Overhead (+6 more)

### Community 7 - "Spotify API Integration"
Cohesion: 0.16
Nodes (15): SpotifyTrack, Int, List, String, SpotifyAlbumItem, SpotifyArtistItem, SpotifyAuthApi, SpotifyImageItem (+7 more)

### Community 8 - "Session Management & Tests"
Cohesion: 0.12
Nodes (9): AndroidSessionManager, UserSession, DummyCodeVerifierCache, DummySessionManager, ExampleRobolectricTest, String, UserSession, CodeVerifierCache (+1 more)

### Community 9 - "Spotify Profile Sync Service"
Cohesion: 0.21
Nodes (10): Boolean, Int, List, String, ProfileSyncService, SpotifyArtistDto, SpotifyTopTracksResponse, SpotifyTrackDto (+2 more)

### Community 10 - "FM Synth Audio Engine"
Cohesion: 0.39
Nodes (4): String, ProfileSynthEngine, AudioTrack, Job

## Knowledge Gaps
- **18 isolated node(s):** `Inject`, `Singleton`, `SpotifyArtistDto`, `SpotifyTrackDto`, `SpotifyTracksList` (+13 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SparkViewModel` connect `ViewModel & Profile State` to `Room Database & DAOs`, `MainActivity & Profile UI`, `Compose Screens (Chat/Admin)`, `Chat & Swipe Repository Logic`, `Auth & Login Flow`, `Session Management & Tests`?**
  _High betweenness centrality (0.412) - this node is a cross-community bridge._
- **Why does `Spark Optimization Audit` connect `Auth & Login Flow` to `Room Database & DAOs`, `Chat & Swipe Repository Logic`, `Supabase Service & Secure Prefs`, `Spotify API Integration`?**
  _High betweenness centrality (0.221) - this node is a cross-community bridge._
- **Why does `SystemLog` connect `Chat & Swipe Repository Logic` to `Room Database & DAOs`, `ViewModel & Profile State`, `Supabase Service & Secure Prefs`, `Spotify API Integration`, `Spotify Profile Sync Service`?**
  _High betweenness centrality (0.129) - this node is a cross-community bridge._
- **What connects `Inject`, `Singleton`, `SpotifyArtistDto` to the rest of the system?**
  _18 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Room Database & DAOs` be split into smaller, more focused modules?**
  _Cohesion score 0.08292682926829269 - nodes in this community are weakly interconnected._
- **Should `ViewModel & Profile State` be split into smaller, more focused modules?**
  _Cohesion score 0.09411764705882353 - nodes in this community are weakly interconnected._
- **Should `MainActivity & Profile UI` be split into smaller, more focused modules?**
  _Cohesion score 0.08831908831908832 - nodes in this community are weakly interconnected._