# M21 Control Deck — Combined Batches 1 + 2

An unofficial, offline-first Android launcher and local music player designed for the **FiiO M21** (Android 13). The approved black/orange concept is included in `design/approved-concept.png`.

> **Status:** Source implementation provided. **Not yet compiled or tested on a physical M21.** The ZIP is a project, **not an APK**. Do not assume that the audio path is bit-perfect or that Android can remap the FiiO HOLD/fn switches.

## Implemented in source

- Android HOME + LAUNCHER intents, so it can be chosen as the default home screen while keeping the stock launcher available.
- Jetpack Compose dark/amber dashboard with album art, playback metadata, transport, volume, EQ and mode knobs, Local/Streaming selector, and bottom Library / Playlists / Settings dock.
- FiiO-branded button opens Quick Deck with real app, queue, audio, Bluetooth and display shortcuts.
- Android Media3 ExoPlayer + MediaSessionService for playback, notification/media controls, background operation and standard headset/media-button integration.
- Music access through scoped Android MediaStore rather than broad file access: library scan, SD card aggregation, album, artist, folder, favorites and recent views, search.
- Queue, seek, shuffle, repeat all/one, pause, resume, next/previous; drag rotary volume interaction, touch volume control and optional haptics.
- Local SQLite favorites, playlist create/rename/delete, add/remove songs, move playlist order, M3U import/export (MediaStore URI format).
- Streaming selector launches installed Spotify, TIDAL, Apple Music and YouTube Music apps; global streaming session takeover is **not** implemented.
- UI settings for compact layout, haptics and volume-knob sensitivity; shortcut to Android default-home picker; audio-effects/settings fallback.
- CI configuration that builds an installable **debug APK** as a downloadable GitHub Actions artifact.

## Build APK with GitHub Actions

1. Create a **separate** GitHub repository named `M21ControlDeck`. Do **not** upload into BoreLab or the handheld-console launcher repository.
2. Upload **all the files at this ZIP's root**, including `.github/workflows/build-apk.yml`, and commit to the `main` branch. Include hidden files.
3. Open **Actions → Build M21 APK → Run workflow** (or let the push run build).
4. When the build succeeds, open the workflow run's **Artifacts** and download `M21ControlDeck-Batch1-2-debug-apk`. Unzip and install `app-debug.apk` on your M21.
5. Open M21 Control Deck, allow access to music, scan storage, and select it as your Android home launcher if desired.

This process signs the APK with Android's standard debug key. Debug builds are for device testing; never treat them as production signed releases.

### Build locally

Open the project in Android Studio (JDK 17, Android SDK 35, Gradle 8.9), let Gradle sync, then **Build → Build APK(s)**. Or run `gradle :app:assembleDebug` using Gradle 8.9.

## Feature boundaries / honest limitations

- **FiiO PEQ:** Tapping EQ attempts the Android audio-effects panel and falls back to sound settings. FiiO's system-wide PEQ may require manual `Settings → Audio → Equalizer`. This build does not contain a separate 10-band audio DSP.
- **Bit-perfect/DSD:** Media3 may output decoded PCM through Android mixers and DSP; high-resolution FLAC playback is not proof of lossless output path. Verify the output sample rate/bit depth on M21 hardware before claiming bit-perfect.
- **Hardware controls:** Standard media keys should work when received by Android. Special FiiO multifunction and HOLD behavior is firmware-specific and has not been mapped.
- **MicroSD & album art:** MediaStore aggregation and album art permissions/URIs need M21 hardware testing. If art isn't available, the included placeholder is shown.
- **Genres:** MediaStore scanning currently supports songs, artists, albums and folders; full genre indexing is not yet coded.
- **Advanced formats:** Media3 handles standard supported formats (including MP3, AAC, WAV and many FLAC files), but ALAC/DSD support is not promised without adding/testing extensions.
- **Playback resumption:** In-memory queue and normal background playback work; persistent playback queue/resume across device reboots is not implemented.
- **Global streaming controls:** The Streaming selector launches installed apps; their metadata and remote queue control belong to the future integration phase.
- **M3U portability:** Export uses Android content URIs; another device or non-Android player might not resolve them. Import can also match by filename if available.
- **One-theme release:** Only the approved orange/black design ships. Additional themes are future work.

## Test this before setting it as default home

1. Grant music read permission. Confirm SD card and internal music are scanned and playable.
2. Test 3.5 mm and 4.4 mm outputs and verify output volume behavior in headphone mode. **Avoid line-out testing at high gain.**
3. Confirm the device's physical play/pause, next/previous, volume and HOLD controls; special behavior may differ.
4. Try EQ panel; on devices that don't expose it, open the FiiO Audio menu manually.
5. Lock the screen; ensure media notification and playback continue. Disconnect headphones and confirm pause behavior.
6. Create a playlist, add songs, move tracks, export/import M3U, then relaunch app to confirm persistence.
7. Set home launcher, press Home, then go to Android Settings to restore the default FiiO launcher. Do not lose access to the stock settings.

## Architecture

- `MainActivity.kt`: permission gate, Compose entry point.
- `ui/Home.kt`: approved tactile launcher UI and controls.
- `ui/Pages.kt`: library, playlists, queue, EQ, Settings and app drawer.
- `ui/DeckState.kt`: local state, SQLite persistence and scanning.
- `data/Library.kt`: Android MediaStore reading for shared music.
- `data/PlaylistStore.kt`: SQLite playlists/favorites.
- `data/M3u.kt`: M3U import/export.
- `playback/PlaybackService.kt`: Android MediaSessionService + ExoPlayer.
- `playback/DeckPlayer.kt`: controller, queue and system volume functions.

**Privacy:** No account, web service, analytics SDK, network permission or cloud dependency.

**Trademark notice:** Independent personal project. Not affiliated with or endorsed by FiiO.


### Note on artwork
The original approved concept art is in the downloadable project ZIP; a compact vector placeholder is used inside the app until the player can retrieve album artwork from MediaStore.
