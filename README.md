# life.os — Android launcher

A native Jetpack Compose / Kotlin port of the "Neo Editorial" life.os mockup, built as a real Android home-screen launcher.

## What's real
| Area | Behaviour |
|---|---|
| Tasks, Routines, Inbox | Fully editable, stored on-device with Room. Tap any row/card to edit or delete. Routines reset every day. |
| Projects | Each project has its own page: add and tick off tasks, set a deadline, and mark it complete. Progress is worked out from the project's tasks. Tasks can also be assigned to a project from the task editor. |
| Quick capture (＋) | Task → Tasks list, Note → Inbox, Event → opens your calendar app pre-filled. |
| Focus card | Starts a countdown from the task's estimate (25 min default); "Mark done" completes it. |
| Calendar | Reads your phone's calendars (READ_CALENDAR). Month grid, per-day agenda, tap an event to open it, "+ Add event" opens the calendar app. |
| Music | Reads the active media session (Spotify, YouTube Music, …) and controls play/pause/next/previous/±10 s. Needs **Notification access** once. When nothing is playing, tapping the Now Playing card (or the Music page artwork) opens your default music app. |
| App drawer | Tap the grid icon (top right) or swipe up on the bottom bar. Search, long-press for App info / Uninstall. |
| Swipe paging | Swipe left / right anywhere on a page to go to the next / previous page (in your chosen order). |
| Essentials page | Pin your go-to apps (+ button, or long-press any app in the drawer). Tap to open, long-press to unpin. |
| Settings | Tap your initials (top right): edit initials, glass transparency slider, default music app, and page order (move pages up/down). |
| Home button | Behaves like a launcher: returns to the Home tab and closes the drawer. |

Sample tasks/routines/projects are seeded on first launch — delete or edit freely.

## Build with GitHub Actions
1. Create a repo and push this folder's contents.
2. Open **Actions → Build Android APK** (runs on every push; also "Run workflow").
3. Download **life-os-debug-apk** or **life-os-release-apk** from the run's Artifacts.
4. Push a tag like `v1.0` to also attach the APKs to a GitHub Release.

The workflow installs Gradle 8.10.2 itself, so no Gradle wrapper is required. To get a local wrapper: `gradle wrapper --gradle-version 8.10.2`.

The release APK is signed with the debug key so it installs directly; use your own keystore before distributing.

## Build locally
Android Studio (Ladybug or newer), JDK 17 → open the folder → Run. Or `gradle assembleDebug`.

## First-run setup on the phone
1. Install the APK, press Home, choose **life.os** → *Always*  (or Settings → Apps → Default apps → Home app).
2. Music page → **Enable media access** → switch on life.os. On Android 13+ with a sideloaded APK, first open App info → ⋮ → *Allow restricted settings*.
3. Calendar page → **Allow calendar access**.

## Project layout
```
app/src/main/java/com/lifeos/launcher/
  MainActivity.kt        launcher activity (HOME intent filter)
  data/                  Room entities/DAOs, calendar provider reader
  media/                 MediaSession reader + notification-listener service
  apps/                  installed-app list + launch helpers
  ui/                    theme, view-model, screens, dialogs, app drawer
```

## Differences from the HTML mockup
- Weather chip → battery level (no network/location permission needed).
- Music shuffle/repeat → ±10 s seek (Android's media controls don't expose shuffle/repeat generically); "Up next" queue → "Playing from" with an Open button.
- Desktop sidebar/terminal removed; the app drawer replaces it.
