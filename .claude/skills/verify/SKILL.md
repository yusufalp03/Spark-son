---
name: verify
description: Spark-son Android uygulamasında bir kod değişikliğinin gerçekten çalıştığını doğrula - derleme, unit testler (Robolectric dahil) ve Android lint. Commit'ten önce her önemli değişiklikte kullan.
---

# Spark-son Doğrulama

Bu proje bir Android uygulamasıdır (Kotlin + Jetpack Compose, Supabase, Spotify App
Remote). Bu ortamda emülatör yoktur; doğrulama JVM üzerinde yapılır: derleme,
Robolectric/JUnit testleri ve Android lint.

## Ön koşul

`ANDROID_HOME` ayarlı değilse ortam kurulumunu SessionStart hook'u yapar; elle
gerekirse: `CLAUDE_CODE_REMOTE=true .claude/hooks/session-start.sh` çalıştır ve
komutları `ANDROID_HOME="$HOME/android-sdk"` önekiyle çağır.

## Adımlar

1. **Derleme** — değişikliğin dokunduğu kod derleniyor mu:

   ```bash
   ./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin
   ```

2. **Unit testler** — tamamı ~2 dk sürer; önce değişikliğe en yakın test sınıfını
   `--tests` ile çalıştır, sonra tam süit:

   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

   Başarısızlıkta rapor: `app/build/reports/tests/testDebugUnitTest/index.html`

3. **Lint** — Compose/Android API yanlış kullanımlarını yakalar:

   ```bash
   ./gradlew :app:lintDebug
   ```

   Rapor: `app/build/reports/lint-results-debug.html`

4. **Davranış doğrulaması** — sadece derleme yetmez. Değiştirilen davranışı
   gözlemleyen bir test yoksa, Robolectric ile küçük bir test yaz
   (`app/src/test/java/com/example/`), çalıştır ve değişiklikle birlikte commit'le.
   UI/ViewModel akışları için `kotlinx-coroutines-test` ve Robolectric zaten
   bağımlılıklarda mevcut.

5. **APK paketleme** (yalnızca manifest, kaynak dosyaları veya Gradle yapılandırması
   değiştiyse):

   ```bash
   ./gradlew :app:assembleDebug
   ```

## Doğrulanamayanlar (dürüst raporla)

- Gerçek cihaz gerektiren akışlar: Spotify App Remote çalma, gerçek Supabase
  oturumu, `androidTest` altındaki Espresso testleri. Bunlar değiştiyse sonucu
  "cihazda doğrulanmadı" diye açıkça belirt.
- Supabase şema değişiklikleri: `supabase/schema.sql` düzenlendiyse SQL'i canlı
  projeye uygulamadan doğrulama tamamlanmış sayılmaz.

## Bitiş

Değişiklikten sonra bilgi grafiğini güncelle: `graphify update .`
