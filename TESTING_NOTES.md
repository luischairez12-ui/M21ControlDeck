# M21 Control Deck — On-device validation

## First hardware-pass checklist

- [ ] Gradle CI returns a successful `assembleDebug`
- [ ] Install debug APK under Android Mode
- [ ] Permission gate grants READ_MEDIA_AUDIO
- [ ] Both internal and inserted microSD songs appear
- [ ] Play MP3, FLAC, AAC and WAV samples (multiple sample rates)
- [ ] Audio stays playing while screen is off
- [ ] Headphone unplug pauses (normal headphones)
- [ ] Real volume rocker and touch dial both adjust audio without unsafe jumps
- [ ] FiiO HOLD switch still behaves normally; no promise of interception
- [ ] Music notification offers transport controls
- [ ] Shuffle / repeat one / repeat all work
- [ ] Local SQLite playlist changes persist after app restart
- [ ] Album / artist / folder searches open expected tracks
- [ ] M3U import-export resolves tracks by MediaStore URI on same device
- [ ] External audio-effects panel / system sound settings opens
- [ ] Spotify/TIDAL launch only when installed
- [ ] Home launcher picker allows restore of stock launcher
- [ ] SD removal / revoked permission doesn't crash player

## Scope to revisit after first device APK

- FiiO native sample-rate/bit-perfect output validation
- M21's global EQ deep-link (only if FiiO exposes one)
- MediaStore album art on device vs fallback cover
- Collapsed screen vertical spacing at native 750×1334
- Android 13 background process/notification behavior
- SD card identity changes and playlist robustness
- Audio session control for streaming mode
