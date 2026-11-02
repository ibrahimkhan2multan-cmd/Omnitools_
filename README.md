# OmniTools — All-in-One Utility Suite

A native Android app (Kotlin, Jetpack Compose, Material 3) that bundles everyday utilities. Everything runs on-device: no account, no analytics, no ads, no backend.

**No Android Studio needed.** GitHub Actions compiles the APK for you on every push.

## Get the APK

1. Push a change to `main` (or open the **Actions** tab ▸ **Android Native CI/CD** ▸ **Run workflow**).
2. Open the finished run and download the **`debug-apk`** artifact (kept 7 days).
3. Unzip it and install `app-debug.apk` on your phone (allow "install unknown apps" for your browser/files app).

The run also executes the unit tests and uploads a **`unit-test-report`** artifact. The APK is uploaded *before* the tests run, so a failing test turns the run red but never hides the APK.

> **Build status:** the debug APK compiles and all 34 JVM unit tests pass in CI. The app has **not yet been run on a real device**, so UI behaviour, camera scanning, permission handling, sharing and saving still need manual testing on a phone. Release (R8-minified) builds are untested.

## What's in Part 1

| Tool | What it does |
|---|---|
| QR & Barcode Scanner | CameraX + on-device ML Kit. Camera permission requested only when the scanner opens. Never opens anything automatically; links are analysed (http, IP address, shortener, punycode, `user@` tricks, odd ports) and need confirmation. Local history with delete and clear-all. |
| QR Code Generator | Text, web link, Wi-Fi and contact (vCard) codes via ZXing, fully offline. Save as PNG, share, copy. |
| Notepad | Multiple notes, search, autosave, copy/share/delete. Stored as private JSON files. |
| Text Tools | Counts, case, whitespace cleanup, duplicates, sort, reverse, line numbers, find & replace, Base64, URL encode/decode, JSON format/minify/validate. |
| Calculator | Standard + scientific (brackets, `%`, `^`, `!`, roots, trig DEG/RAD, ln/log). |
| Unit Converter | Length, weight, area, volume, speed, data size (decimal vs binary), temperature, number bases. |
| Device Information | Model, Android/API, ABIs, screen, RAM/storage snapshot, battery, sensors, cameras. App-only values are shown separately. |
| Password Generator | `SecureRandom`, entropy estimate, plus UUID generator. Nothing is stored. |

Dashboard: search, recents, favorites, suggested tools, category screens, light/dark/system theme, settings, About & privacy.

**Not built yet:** PDF reader / image-to-PDF, image tools, file manager, ZIP manager, code editor and HTML/JS runner, productivity timers, premium/trial framework. Categories with no tools yet are hidden instead of showing dead buttons.

## Project layout

```
app/src/main/java/com/omnitools/app/
  MainActivity.kt, OmniApp.kt     entry point and navigation
  data/                           Prefs (DataStore), ToolRegistry
  ui/                             theme, shared components, home, settings, ToolHost
  tools/{qr,notes,text,calc,device,password}/
  util/                           clipboard/share helpers, formatting
app/src/test/                     JVM unit tests
.github/workflows/android.yml     builds the APK (and a signed release on tags)
bash/                             helper scripts (push to build, generate signing keys)
fastlane/metadata/                store listing text, privacy policy
```

## Toolchain (changed from the original Hello World template)

| | Template | Now | Why |
|---|---|---|---|
| Android Gradle Plugin | 8.2.0 | 8.7.3 | compileSdk 35 libraries |
| Kotlin | 1.9.20 | 2.0.21 + Compose compiler plugin | Compose |
| Gradle (CI) | 8.5 | 8.9 | required by AGP 8.7 |
| compileSdk / targetSdk | 34 | 35 | AndroidX/CameraX requirements |
| minSdk | 24 | 26 | `java.util.Base64`, adaptive icons |
| UI | XML views | Jetpack Compose | |

The signing config, build types (debug uses the `.debug` application-id suffix) and tag-triggered signed release are unchanged from the template.

## Signed release APK (optional)

```bash
cd bash
./utils-generate-keystore
```

Add the printed secrets in the repo's **Settings ▸ Secrets and variables ▸ Actions**, then push a tag such as `v0.1.0`. The workflow builds `app-release.apk` and attaches it to a GitHub release. Release builds use R8 shrinking and have not been tested yet; if a release build misbehaves where debug works, add the missing keep rule to `app/proguard-rules.pro`.

## Local build (optional)

Open the folder in Android Studio and let it sync, or with JDK 17 + Android SDK 35 and Gradle 8.9: `gradle assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`; `gradle testDebugUnitTest` for tests.

The package id `com.omnitools.app` is a placeholder; change `namespace` and `applicationId` in `app/build.gradle` before any public release.

## Privacy

Notes, favorites, recents and scan history stay on the device. The only permission is CAMERA. See `fastlane/metadata/android/en-US/privacy_policy.md` (fill in the contact placeholders before publishing).

## Roadmap

See [TODO.md](TODO.md).
