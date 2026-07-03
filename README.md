# Spark 🎵⚡

İnsanları fotoğraf yerine **müzik zevkiyle** eşleştiren flört uygulaması. Her profilin bir "imza şarkısı" vardır: kullanıcı Spotify'dan seçtiği şarkının en can alıcı 30 saniyelik kesitini kırpar; keşfet ekranında kartı gören kişi bu kesiti cihazındaki Spotify uygulaması üzerinden dinler (Spotify Premium gerektirir; çalınamazsa türe göre üretilen melodiye düşülür). Karşılıklı beğeni gerçek zamanlı sohbete dönüşür.

**Teknolojiler:** Kotlin, Jetpack Compose, Room (offline-first), Supabase (Auth + Postgres + Realtime), Spotify API.

---

## 1) Gereksinimler

- [Android Studio](https://developer.android.com/studio) (güncel sürüm)
- Bir [Supabase](https://supabase.com) projesi (ücretsiz plan yeterli)
- Bir [Spotify Developer](https://developer.spotify.com/dashboard) uygulaması

## 2) Supabase kurulumu

1. [supabase.com](https://supabase.com) üzerinde yeni bir proje oluşturun.
2. **SQL Editor**'ü açın ve bu repodaki [`supabase/schema.sql`](supabase/schema.sql) dosyasının tamamını çalıştırın. Bu; `profiles`, `likes`, `matches`, `messages`, `feedback` tablolarını, RLS güvenlik kurallarını ve keşif/eşleşme RPC fonksiyonlarını oluşturur.
3. **Authentication → Providers → Spotify**'ı etkinleştirin:
   - Spotify Developer Dashboard'dan aldığınız **Client ID** ve **Client Secret** değerlerini girin.
4. **Authentication → URL Configuration → Redirect URLs** listesine şunu ekleyin (login'in çalışması için şart):
   ```
   spark://login
   ```
5. **Project Settings → API**'den `Project URL` ve `anon public` anahtarını not edin.

## 3) Spotify Developer kurulumu

1. [developer.spotify.com/dashboard](https://developer.spotify.com/dashboard) üzerinde bir uygulama oluşturun.
2. Uygulamanın **Redirect URIs** alanına Supabase callback adresinizi ekleyin:
   ```
   https://<proje-id>.supabase.co/auth/v1/callback
   ```
3. Client ID/Secret değerlerini hem Supabase Spotify provider'ına (yukarıdaki adım) hem de `.env` dosyasına girin (şarkı arama API'si için).

> Not: Spotify uygulaması "Development Mode"da iken yalnızca panelden eklediğiniz test kullanıcıları giriş yapabilir. Herkese açmak için Spotify'dan **Extended Quota Mode** talep edin.

### İmza şarkısı kesiti (App Remote)

- Kesit çalma, cihazda yüklü **Spotify uygulaması** üzerinden yapılır ve
  belirli bir parçayı isteğe bağlı çalmak **Spotify Premium** gerektirir.
  Çalınamazsa uygulama türe göre üretilen melodiye (synth) geri düşer.
- Spotify Dashboard'daki **Redirect URIs** listesinde `spark://login`
  adresinin de bulunması gerekir (App Remote yetkilendirmesi bunu kullanır).
- Gerekli `spotify-app-remote` AAR'ı Maven'da yayınlanmaz; ilk derlemede
  GitHub releases'tan **otomatik indirilir** (`app/libs/` altına). İndirme
  başarısız olursa hata mesajındaki adresten elle indirip aynı klasöre koyun.

## 4) Uygulamayı derleme

1. Projeyi Android Studio'da açın.
2. Proje köküne `.env` dosyası oluşturun (örnek için `.env.example`):
   ```
   SPOTIFY_CLIENT_ID=...
   SPOTIFY_CLIENT_SECRET=...
   SUPABASE_URL=https://<proje-id>.supabase.co
   SUPABASE_ANON_KEY=...
   ```
3. Emülatörde veya gerçek cihazda çalıştırın.

## 5) Yayın (Release) derlemesi

Release imzalaması ortam değişkenleriyle yapılır; keystore yoksa debug anahtarına düşer (Play Store debug imzalı paketleri **reddeder**):

```bash
export KEYSTORE_PATH=/path/to/my-upload-key.jks
export STORE_PASSWORD=...
export KEY_PASSWORD=...
./gradlew bundleRelease
```

Keystore oluşturmak için:
```bash
keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

## Mimari özet

- **Room = yerel tek doğruluk kaynağı (SSOT).** UI yalnızca Room flow'larını dinler; bulut verisi Room'a yazılır.
- **Keşif:** `get_discover_profiles` RPC'si — henüz swipe edilmemiş gerçek kullanıcıları müzik-zevki uyum puanıyla döndürür.
- **Eşleşme:** `handle_swipe` RPC'si — beğeniyi kaydeder, karşılıklı beğeni varsa atomik olarak eşleşme oluşturur.
- **Sohbet:** mesaj önce Room'a `PENDING_INSERT` yazılır, buluta eşitlenir (offline-first; başarısızsa "yeniden gönder"); karşı tarafın mesajları Supabase Realtime ile anında düşer.
- **Spotify verisi:** OAuth sonrası kullanıcının gerçek top artist/track/tür verisi profile işlenir ve keşif uyum puanında kullanılır.
- **Ses:** İmza şarkısı kesiti, cihazdaki Spotify uygulamasıyla tam şarkının içinden çalınır (`SpotifyRemotePlayer`, App Remote SDK; Premium gerekir). Çalınamazsa profilin türüne göre gerçek zamanlı FM sentezine düşülür (`ProfileSynthEngine`).

## Giriş (login) sorun giderme

Spotify ile giriş çalışmıyorsa sırasıyla kontrol edin:

1. `spark://login` Supabase **Redirect URLs** listesinde mi? (En yaygın sorun budur — eklenmemişse tarayıcı, girişten sonra uygulamaya geri dönemez ve login ekranında takılı kalırsınız.)
2. Spotify Dashboard'daki Redirect URI tam olarak `https://<proje-id>.supabase.co/auth/v1/callback` mi?
3. Supabase **Providers → Spotify** etkin ve Client ID/Secret doğru mu?
4. `.env` içindeki `SUPABASE_URL` / `SUPABASE_ANON_KEY` doğru mu? (Eksikse login ekranında sarı uyarı kartı görünür.)
5. Spotify uygulamanız Development Mode'daysa, giriş yapan hesap panelde test kullanıcısı olarak ekli mi?
