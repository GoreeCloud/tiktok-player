# Native direct-download acceptance

**Workstream:** GoreeCloud Clips / tiktok-player.

## Automated checks
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:lintDebug`
- `./gradlew :app:assembleDebug`
- Check the exact commit SHA and successful artifact upload.

## Development APK install behavior

GitHub Actions debug builds are labeled **GoreeCloud Clips Dev** and install as
`com.goreecloud.clips.dev`, separate from the existing `com.goreecloud.clips` app.
GitHub runner debug signing keys can change between runs; do not uninstall the
existing owner app or claim an in-place upgrade has passed. Release signing
continuity requires its own validated process.

## Real-device checks
- [ ] Launch light UI on Android 15/16; title and top controls begin below the status-bar icons/camera cutout, with no overlap.
- [ ] Verify dark mode and the status/navigation icon contrast on both themes.
- [ ] Verify app-owned buttons use icons only across Explore, Saved, Settings, and confirm/cancel dialogs; native glyphs have TalkBack labels and tooltips.
- [ ] Verify touch targets are at least 48 dp, show focus, and bottom navigation stays above the gesture navigation area.
- [ ] Open keyboard while editing provider URL and confirm the action and navigation are not obscured.
- [ ] Browse and play an accessible TikTok video.
- [ ] Tap Download on a video: copyright confirmation appears immediately, with no provider setup.
- [ ] Download an owner-controlled video using the bundled on-device engine.
- [ ] Verify playable file in Movies/GoreeCloud Clips on Android 10+.
- [ ] Confirm download from a saved-video entry and a pasted short share link.
- [ ] Verify media content is a real video, not an error document.
- [ ] Verify failed/unavailable content leaves no partial files in the public library.
- [ ] Verify optional provider fallback requires separate explicit consent.
- [ ] Test invalid URLs, repeated taps, offline mode, storage failure and app restart.
- [ ] Confirm Android 8–9 behavior and native library compatibility for supported ABIs.

**Unverified:** live TikTok extractor success, app installation and playback. CI alone is insufficient to mark the app stable.
