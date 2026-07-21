# My Medical Journal

An **offline-first** Android health records app built with Jetpack Compose. Track vitals, medications, lab results, and appointments for yourself and your family — all data stays on your device.

**العربية مدعومة** — the entire app can switch between English and Arabic from Settings.

## Features

### Health Tracking
- **Blood Pressure** — systolic/diastolic/heart rate with danger alerts and pattern analysis charts (correlates BP with sleep, smoking, and activity)
- **Blood Sugar** — supports both **mg/dL and mmol/L** units with correct reference ranges and status classification per unit and category (Fasting, Post-Prandial, Random, Bedtime)
- **Weight, Sleep, Symptoms, Mood** — full history with charts
- **Medications** — active medication list, dose logging, and daily reminder notifications
- **Lab Results** — grouped by test name + unit, with trend charts showing the reference range band and out-of-range highlighting
- **Lifestyle** — water intake (with daily goal), smoking, exercise

### Family Profiles
- Add family members / children with full profiles (blood type, conditions, allergies, emergency contact, height, weight)
- Per-profile export: select any profile and export its data as JSON
- Share directly to your doctor via **WhatsApp** or any share target

### Appointments
- Calendar of doctor visits, medication schedules, and lab appointments
- Upcoming / past views with mark-complete

### Privacy & Data
- **100% local storage** (Room database) — no cloud, no account, no internet required
- Local backup/restore to a JSON file you choose
- Export TXT / CSV / JSON / PDF reports
- First-run onboarding wizard sets up your profile

### AI Assistant (optional)
- Chat and weekly health summaries via Gemini or OpenAI — requires entering your own API key in Settings; works fully offline without it

## Run Locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio) (latest stable)

1. Open Android Studio → **Open** → choose this project directory
2. Let Gradle sync finish
3. Run the app on an emulator or physical device (min SDK 24 / Android 7.0)

## CI/CD — APK Builds via GitHub Actions

Every push to `main` or a `claude/**` branch, and every pull request, triggers the **Build Signed APK** workflow (`.github/workflows/build-signed-apk.yml`):

| Job | Output | Signing | Requirement |
|---|---|---|---|
| `build-debug` | `debug-apk-*` artifact | Debug keystore (in repo) | None — always runs |
| `build-release` | `release-apk-*` artifact | Your release keystore | Secrets below |
| `notify-pr` | PR comment with direct download links | — | Runs on PRs |

### Downloading an APK

1. Go to the **Actions** tab → click the latest **Build Signed APK** run
2. Scroll to **Artifacts** and download `debug-apk-*` (or `release-apk-*`)
3. On pull requests, a bot comment posts direct download links automatically

> Artifact downloads require being signed in to GitHub with access to this repository. Debug artifacts expire after 7 days, release after 14.

### Enabling Release Builds

The release job is skipped until signing credentials are configured. In the repo go to **Settings → Secrets and variables → Actions**:

**Secrets** (Secrets tab → *New repository secret*):

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 my-upload-key.jks` output |
| `STORE_PASSWORD` | Keystore store password |
| `KEY_PASSWORD` | Password for the `upload` key alias |

**Variables** (Variables tab → *New repository variable*):

| Variable | Value |
|---|---|
| `HAS_RELEASE_KEYSTORE` | `true` |

No release keystore yet? Create one (the alias must be `upload`):

```bash
keytool -genkeypair -v -keystore my-upload-key.jks -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

## Tech Stack

- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM — `HealthViewModel` + StateFlow
- **Database:** Room (schema v6, with migrations)
- **Background work:** WorkManager (pattern checks, medication reminders)
- **i18n:** `LocalStrings.kt` — English + Arabic
- **Build:** Gradle 9.3.1, AGP 9.1.1, Kotlin 2.2.10, KSP 2.2.10-2.0.2
