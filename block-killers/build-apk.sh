#!/bin/bash
# Builds release/Zombie-Killer.apk with the plain Android SDK command-line tools (no Gradle needed).
#   ANDROID_HOME  Android SDK location (default: ~/android-sdk; installed automatically if missing)
# Steps: aapt2 compile/link -> javac -> d8 (dex) -> zipalign -> apksigner (v1+v2+v3 signature)
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-$HOME/android-sdk}"
BT_VER=34.0.0
API=34
BT="$SDK/build-tools/$BT_VER"
JAR="$SDK/platforms/android-$API/android.jar"

if [ ! -x "$BT/aapt2" ] || [ ! -f "$JAR" ]; then
  echo "== Installing Android SDK into $SDK"
  mkdir -p "$SDK/cmdline-tools"
  if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
    curl -sSLo /tmp/cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
    unzip -q -o /tmp/cmdtools.zip -d "$SDK/cmdline-tools"
    mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
  fi
  yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "build-tools;$BT_VER" "platforms;android-$API" > /dev/null
fi

B=build/apk
mkdir -p "$B" release
find "$B" -mindepth 1 -delete

echo "== Compiling resources"
"$BT/aapt2" compile --dir app/src/main/res -o "$B/res.zip"
# Gradle supplies the package via "namespace"; inject it for the plain-SDK build.
sed 's|<manifest xmlns:android="http://schemas.android.com/apk/res/android"|<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.blockkillers"|' \
  app/src/main/AndroidManifest.xml > "$B/AndroidManifest.xml"
"$BT/aapt2" link -o "$B/base.apk" -I "$JAR" --manifest "$B/AndroidManifest.xml" \
  --java "$B/gen" --min-sdk-version 24 --target-sdk-version $API --version-code 4 --version-name 3.1 -A app/src/main/assets "$B/res.zip"

echo "== Compiling Java"
mkdir -p "$B/classes"
javac -encoding UTF-8 --release 11 -Xlint:-options -cp "$JAR" -d "$B/classes" \
  $(find app/src/main/java "$B/gen" -name '*.java')

echo "== Dexing"
mkdir -p "$B/dex"
"$BT/d8" --release --min-api 24 --lib "$JAR" --output "$B/dex" $(find "$B/classes" -name '*.class')

echo "== Packaging"
cp "$B/base.apk" "$B/unsigned.apk"
(cd "$B/dex" && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 "$B/unsigned.apk" "$B/aligned.apk"

echo "== Signing"
KS=signing/block-killers.jks
if [ ! -f "$KS" ]; then
  mkdir -p signing
  keytool -genkeypair -keystore "$KS" -storepass blockkillers -keypass blockkillers -alias blockkillers \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Block Killers, O=Block Killers"
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:blockkillers --key-pass pass:blockkillers \
  --out release/Zombie-Killer.apk "$B/aligned.apk"
rm -f release/Zombie-Killer.apk.idsig
"$BT/apksigner" verify --verbose release/Zombie-Killer.apk | head -4
ls -l release/Zombie-Killer.apk
echo "== Done: release/Zombie-Killer.apk"
