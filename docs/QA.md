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
For side-by-side viewport testing, the Video Player Device QA workflow builds
`com.goreecloud.clips.videoqa` using `-PqaSuffix=.videoqa`, so the existing
standard Dev and owner app packages are untouched.
GitHub runner debug signing keys can change between runs; do not uninstall the
existing owner app or claim an in-place upgrade has passed. Release signing
continuity requires its own validated process.

## Video-first Explore layout checks

- [ ] Compare WebView viewport bounds with previous release on 1080×2400 phone; verify video begins directly below the status bar, not below an app title or URL strip.
- [ ] Check Explore, Saved, Recent, Settings and the bottom-left floating glyph; the popup overlays all eight video tools without shrinking or reloading WebView.
- [ ] Test actions and verify overlay closes after use; Back dismisses it before navigating the site.
- [ ] Confirm return from Saved/Settings restores expanded video and initially hides actions.
- [ ] Verify at least 48 dp touch targets and spoken labels in light and dark mode.

## Refined dock and unobstructed controls checks

- [ ] On the OnePlus 1080x2400 screen, prove that the action button bounds begin *below* the TikTok WebView's bottom boundary, leaving TikTok's native Home control uncovered.
- [ ] Compare the new dock's visual cohesion: one pill housing Explore, Saved, Recent and Settings, separate circular action on the left; no individually stroked tab boxes.
- [ ] All five icon-only controls have 48dp+ targets, clear content descriptions and selected-tab state in dark/light modes.
- [ ] Press the dock's left glyph to open the upward menu; Back closes it; opening from Saved/Recent/Settings returns to Explore and expands it.
- [ ] Video WebView retains safe top inset and does not resize when the context menu opens.

## Floating tools and clean browsing checks

- [ ] Floating three-dot action appears at the bottom-left above navigation and is 56dp with TalkBack label and modern outline glyph.
- [ ] The two-row context panel opens upward and stays within system bar bounds; Android Back and action taps close it.
- [ ] Video WebView remains at the same size and is not reloaded when panel expands/collapses; existing Saved, Recent and Settings navigation works.
- [ ] Network layer blocks known third-party ad subresource domains but never TikTok video/CDN or top-level navigation.
- [ ] Document-level TikTok Lite popup and ad-slot rules handle page mutations, not only initial navigation.
- [ ] Verify real TikTok page against known Lite modal and sponsored content; do not label the blocker complete for unknown future site changes.
- [ ] Verify the rule does not hide legitimate TikTok video cards, block media playback or create a reload loop.

## Feature expansion checks

- [ ] More now shows two rows of icon-only actions: Home, Save, Download, Share and Back, Forward, Refresh, Copy Link. Confirm menu does not resize video.
- [ ] Back/Forward respects WebView history; Refresh reloads current page; Copy Link puts current TikTok URL on clipboard.
- [ ] Saved list search filters case-insensitively by title or URL without erasing search focus; tapping favorite moves an item to the top, and favorite persists across reopen.
- [ ] Recent tab records only navigated TikTok video URLs, not home pages; search, reopen, save, download and remove work.
- [ ] Recent Settings toggle stops new records; clear history does not erase Saved entries or browser cookies.
- [ ] Keep-screen-awake preference applies immediately and persists; off is the initial default.
- [ ] Light/dark status-bar contrast, bottom controls and original video viewport remain intact.

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
