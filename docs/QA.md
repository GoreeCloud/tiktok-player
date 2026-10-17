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

## Context action icons and launcher identity (0.9.0-dev)

- [ ] All twelve context actions display a distinct vector icon to the left of readable text in the same row, in both light and dark modes.
- [ ] Each action has a minimum 56dp touch target, spoken name and explanation, with the decorative glyph excluded from TalkBack.
- [ ] The centered menu remains scrollable, dismisses by Back, and the WebView remains full-viewport without an app bar.
- [ ] The adaptive and round launcher icons display the cyan-to-magenta play mark; Android 13+ themed icons use the monochrome mark.
- [ ] An isolated `.iconqa` APK installs without overwriting the original package; test launcher icon, long-press, scroll and full menu actions.

## Compact centered menu (0.8.1-dev)

- [ ] Long-press video: menu opens in the center, never anchored to the bottom.
- [ ] Dialog maximum width 365dp with 20dp minimum side margins; height at most 480dp or 60% of the screen.
- [ ] Enter: 210ms fade/scale (0.95 to 1.0); exit: 150ms fade/scale (1.0 to 0.97), including Back and outside taps.
- [ ] All twelve actions remain scrollable; rows at least 52dp tall.
- [ ] Verify accessibility, focus, theme contrast, small screens and rotation.

## Gesture-only navigation (0.8.0-dev)

- [ ] Confirm WebView is the **only** Explore content child, fills the system-bar-safe bounds, and has no GoreeCloud navigation bar, floating button, header, dock, glyph or overlay before interaction.
- [ ] Press and hold on TikTok WebView for ~500ms without moving; a native text-first context sheet appears with clear grouped actions.
- [ ] Verify every requested action: Saved library, Recent history, Android Downloads, Settings, Save video, Download video, Share link, Copy link, Home, Back, Forward and Refresh.
- [ ] Swipe/scroll/tap TikTok normally without triggering the menu; moving beyond touch slop cancels pending long-press; multi-touch cancels.
- [ ] Android Back dismisses menu; tapping a menu action dismisses it and performs the action; no duplicate browser text-selection popup.
- [ ] Secondary screens have no bottom navigation. Android Back returns to video; a long press on the header opens the context sheet for navigating between Saved, Recent and Settings.
- [ ] Saved and Recent use text-labeled action pills, Settings has labeled touchable rows, and Android confirmations show words instead of hand-drawn glyphs.
- [ ] Verify the one-time gesture discovery toast does not persist over video or repeat on subsequent app launches.
- [ ] Verify light/dark theme contrast, navigation bar insets, menu scroll bounds, screen rotation and Android keyboard interactions.
- [ ] Verify privacy protection and direct download prompts remain unchanged from the prior release.

## Clean browsing regression

- [ ] Network layer blocks known third-party ad subresources while allowing TikTok video/CDN and main-frame navigation.
- [ ] TikTok Lite and sponsored-card DOM suppression handles SPA mutations; do not claim guarantees for future TikTok site changes.
- [ ] Verify ads blocker does not hide normal video cards or break media playback.

## Real-device checks
- [ ] Launch light Explore UI on Android 15/16; TikTok content begins below the status-bar/camera cutout, with no app toolbar.
- [ ] Verify dark mode and the status/navigation icon contrast on both themes.
- [ ] Verify app-owned text buttons and context actions have clear spoken labels, focus and contrast.
- [ ] Verify native context sheet actions and saved/settings text controls are at least 48dp and remain above the system gesture area.
- [ ] Open keyboard while editing provider URL and confirm action rows and dialog remain usable.
- [ ] Browse and play an accessible TikTok video.
- [ ] Long-press the video and choose Download video: copyright confirmation appears when a valid link is recognized.
- [ ] Download an owner-controlled video using the bundled on-device engine.
- [ ] Verify playable file in Movies/GoreeCloud Clips on Android 10+.
- [ ] Confirm download from a saved-video entry and a pasted short share link.
- [ ] Verify media content is a real video, not an error document.
- [ ] Verify failed/unavailable content leaves no partial files in the public library.
- [ ] Verify optional provider fallback requires separate explicit consent.
- [ ] Test invalid URLs, repeated taps, offline mode, storage failure and app restart.
- [ ] Confirm Android 8–9 behavior and native library compatibility for supported ABIs.

**Unverified until device evidence:** live TikTok extractor success and full media playback. CI alone is insufficient to mark the app stable.
