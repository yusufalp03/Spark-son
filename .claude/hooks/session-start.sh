#!/bin/bash
# Claude Code web oturumları için ortam kurulumu: Android SDK + Gradle dağıtımı.
# İdempotenttir; kurulu bileşenleri atlar. Yerel (masaüstü) oturumlarda çalışmaz.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/../.." && pwd)}"
SDK_ROOT="${ANDROID_HOME:-$HOME/android-sdk}"
CMDTOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"
SDK_PACKAGES=("platform-tools" "platforms;android-36.1" "build-tools;36.0.0")

# 1) Android cmdline-tools
if [ ! -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "Android cmdline-tools indiriliyor..."
  tmp=$(mktemp -d)
  curl -fsSL -o "$tmp/cmdtools.zip" "$CMDTOOLS_URL"
  unzip -q "$tmp/cmdtools.zip" -d "$tmp"
  mkdir -p "$SDK_ROOT/cmdline-tools"
  rm -rf "$SDK_ROOT/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
  rm -rf "$tmp"
fi

# 2) Lisanslar + SDK paketleri (kuruluysa sdkmanager hızlıca geçer)
SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
if [ ! -d "$SDK_ROOT/licenses" ]; then
  yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" --licenses >/dev/null 2>&1 || true
fi
missing=()
for pkg in "${SDK_PACKAGES[@]}"; do
  dir="$SDK_ROOT/$(echo "$pkg" | tr ';' '/')"
  [ -d "$dir" ] || missing+=("$pkg")
done
if [ ${#missing[@]} -gt 0 ]; then
  echo "SDK paketleri kuruluyor: ${missing[*]}"
  "$SDKMANAGER" --sdk_root="$SDK_ROOT" "${missing[@]}" >/dev/null
fi

# 3) local.properties ve oturum ortam değişkenleri
if [ ! -f "$PROJECT_DIR/local.properties" ]; then
  printf 'sdk.dir=%s\n' "$SDK_ROOT" > "$PROJECT_DIR/local.properties"
fi
if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  {
    echo "export ANDROID_HOME=\"$SDK_ROOT\""
    echo "export ANDROID_SDK_ROOT=\"$SDK_ROOT\""
  } >> "$CLAUDE_ENV_FILE"
fi

# 4) Gradle dağıtımı: gradle.org, dağıtımları GitHub releases üzerinden sunuyor ve
# github.com bu ortamın ağ politikasında kapalı. Zip, resmi Tencent aynasından
# indirilir, gradle-wrapper.properties'teki distributionSha256Sum ile doğrulanır
# ve wrapper'ın dists dizinine önceden yerleştirilir; ./gradlew indirme yapmaz.
WRAPPER_PROPS="$PROJECT_DIR/gradle/wrapper/gradle-wrapper.properties"
DIST_URL=$(grep '^distributionUrl=' "$WRAPPER_PROPS" | cut -d= -f2- | sed 's/\\//g')
DIST_SHA=$(grep '^distributionSha256Sum=' "$WRAPPER_PROPS" | cut -d= -f2- || true)
ZIP_NAME=$(basename "$DIST_URL")
DIST_BASE="${ZIP_NAME%.zip}"
HASH_DIR=$(python3 - "$DIST_URL" <<'PY'
import hashlib, sys
n = int(hashlib.md5(sys.argv[1].encode()).hexdigest(), 16)
s = ""
while n:
    n, r = divmod(n, 36)
    s = "0123456789abcdefghijklmnopqrstuvwxyz"[r] + s
print(s or "0")
PY
)
DEST="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/$DIST_BASE/$HASH_DIR"
if ! ls "$DEST"/*.ok >/dev/null 2>&1 && [ ! -f "$DEST/$ZIP_NAME" ]; then
  echo "Gradle dağıtımı indiriliyor: $ZIP_NAME"
  tmp=$(mktemp -d)
  curl -fsSL --max-time 120 -o "$tmp/$ZIP_NAME" "$DIST_URL" \
    || curl -fsSL -o "$tmp/$ZIP_NAME" "https://mirrors.cloud.tencent.com/gradle/$ZIP_NAME"
  if [ -n "$DIST_SHA" ]; then
    echo "$DIST_SHA  $tmp/$ZIP_NAME" | sha256sum -c - >/dev/null
  fi
  mkdir -p "$DEST"
  mv "$tmp/$ZIP_NAME" "$DEST/$ZIP_NAME"
  rm -rf "$tmp"
fi

# 5) Isındırma: dağıtımı aç, eklentileri ve derleme bağımlılıklarını çözümle
cd "$PROJECT_DIR"
ANDROID_HOME="$SDK_ROOT" ./gradlew --quiet :app:compileDebugUnitTestKotlin >/dev/null 2>&1 \
  || ANDROID_HOME="$SDK_ROOT" ./gradlew --quiet help >/dev/null

echo "Android ortamı hazır: SDK=$SDK_ROOT, Gradle=$DIST_BASE"
