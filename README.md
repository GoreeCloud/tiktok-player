# GoreeCloud Clips

Native Android browser, saved-video organizer, and built-in direct downloader for TikTok video links. Repository: **GoreeCloud/tiktok-player**.

**Status: experimental development build, not a stable release.** Not affiliated with TikTok.

## Working features

- WebView browser with TikTok navigation, sharing and local bookmarks.
- Light theme by default, optional dark theme, and correct Android status/navigation/cutout/keyboard insets.
- **Full-screen, gesture-driven player:** no GoreeCloud toolbar, bottom navigation, floating button, or persistent control overlay. TikTok's own controls remain unobstructed.
- **Compact centered menu:** video long-press opens a smaller, centered, scrollable dialog with smooth scale-and-fade entry/exit; all actions remain accessible.
- **Long-press context menu:** press and hold the TikTok video area to open a native, scrollable, text-first action sheet containing Saved library, Recent history, Android Downloads, Settings, Save, Download, Share, Copy Link, Home, Back, Forward and Refresh.
- **Accessible navigation on secondary pages:** Android Back returns to the player; long-press the page header to open the same context menu.
- **Readable controls:** Saved, Recent, Settings and confirmation dialogs use accessible text labels instead of custom hand-drawn glyphs.
- **Clean browsing:** known third-party advertising domains are blocked before resource load; a TikTok-only page script suppresses identified ad cards and TikTok Lite promotional dialogs, including content inserted after navigation. These protections are enabled by default. TikTok may change its markup or serve first-party ads that evade these rules.
- **Searchable Saved library:** filter by title, creator or URL, and favorite important videos to keep them first.
- **Private Recent history:** on-device, capped at 150 video URLs, searchable with Save/Download/Open actions; optional history recording can be disabled or cleared in Settings.
- **Playback preference:** opt in to keep the screen awake while watching, in Settings.
- **Direct Download** built into the Android app. No downloader website or provider configuration is required.
- On-device yt-dlp engine attempts to obtain accessible video media after explicit rights confirmation. The app does not transmit user-selected links to a third-party download API for this default path.
- Finished files are published to Android MediaStore under **Movies / GoreeCloud Clips** on Android 10+. On Android 8–9 they remain in the app-specific Movies folder (not automatically indexed by Gallery).
- Progress is handled asynchronously; only one direct download is active at a time.
- Optional Cobalt-compatible processing endpoint in Settings can be used **only after a direct-download failure and a separate explicit retry**. A direct service URL from the provider is handed to Android DownloadManager.
- External cobalt.tools link remains a separate manual option.

## Download a video

1. Open a TikTok video or share its URL to GoreeCloud Clips.
2. **Press and hold the video** to open GoreeCloud's context menu, then choose **Download video**. If the current page is a recognized video URL, the permission confirmation opens; otherwise paste a link.
3. Read and acknowledge the creator/copyright warning.
4. The built-in engine attempts to fetch the video to the device and save it in the video library. No service address needs to be set.

**Limitations:** Direct extraction depends on publicly accessible media and TikTok's current website behavior. The bundled free yt-dlp Android library does **not** implement browser TLS fingerprint impersonation, and some TikTok requests may be blocked. The copyright warning is not a grant of permissions; download only media you own or are permitted to download. Do not bypass access controls or assume every public video may be downloaded. No watermark-removal function is included.

## Optional processing endpoint

Settings can retain an approved HTTPS Cobalt-compatible service for manual fallback. No service is set by default and browser credentials or cookies are not exported to the service.

## Build and verification

Requires JDK 17, Android SDK Platform 35, the Gradle wrapper, and Internet access to Maven Central for the embedded yt-dlp runtime.

```bash
./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Development APK: `app/build/outputs/apk/debug/app-debug.apk` after a successful build. GitHub Actions uploads the debug build artifact.

## Development status

The APK must be installed and tested on a real Android device with authorized test videos. A successful build does not establish TikTok playback or downloader compatibility. See [docs/QA.md](docs/QA.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Licensing

Project-original code is MIT-licensed. The native downloader uses the third-party MIT-licensed `dev.ffmpegkit-maintained:yt-dlp-android:2.0.2` library, which includes bundled components with their own notices. See [THIRD-PARTY.md](THIRD-PARTY.md) and the library's `THIRD-PARTY-NOTICES.txt`. No TikTok code is incorporated.
