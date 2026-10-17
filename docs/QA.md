# Native direct-download acceptance

**Workstream:** GoreeCloud Clips / tiktok-player.

## Automated checks
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:lintDebug`
- `./gradlew :app:assembleDebug`
- Check the exact commit SHA and successful artifact upload.

## Real-device checks
- [ ] Launch light UI, verify readable status and navigation bars.
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
