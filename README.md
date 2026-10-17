# GoreeCloud Clips

Native Android browser, saved-video organizer, and built-in direct downloader for TikTok video links. Repository: **GoreeCloud/tiktok-player**.

**Status: experimental development build, not a stable release.** Not affiliated with TikTok.

## Working features

- WebView browser with TikTok navigation, sharing and local bookmarks.
- Light theme by default, optional dark theme. The native header and bottom bar respect Android status, navigation, cutout and keyboard insets.
- Native icon-only actions throughout the app (including navigation, bookmarks, settings, and confirmation dialogs), with accessible spoken labels and tooltips.
- **Video-first Explore layout:** the WebView fills the area beneath the Android system-bar inset. The title, URL strip and permanent toolbar are removed; the separate bottom dock contains app controls without overlaying TikTok's navigation.
- **Unobstructed floating video tools:** the raised three-dot action sits in the bottom-left app dock, entirely outside the TikTok WebView. It opens a two-row context menu with Home, Save, Download, Share, Back, Forward, Refresh and Copy Link. The closed button never obscures TikTok's own Home or other controls.
- **Refined bottom dock:** one rounded pill groups four glyph-only destinations, with a quiet active selection and consistent 48dp touch targets instead of separately outlined navigation buttons.
- **Clean browsing:** known third-party advertising domains are blocked before resource load; a TikTok-only page script suppresses identified ad cards and TikTok Lite promotional dialogs, including content inserted after navigation. These protections are enabled by default. TikTok may change its markup or serve first-party ads that evade these rules.
- **Searchable Saved library:** filter by title, creator or URL, and favorite important videos to keep them first.
- **Private Recent history:** on-device, capped at 150 video URLs, searchable with Save/Download/Open actions; optional history recording can be disabled or cleared in Settings.
- **Playback preference:** opt in to keep the screen awake while watching, with an icon-only toggle in Settings.
- **Direct Download** built into the Android app. No downloader website or provider configuration is required.
- On-device yt-dlp engine attempts to obtain accessible video media after explicit rights confirmation. The app does not transmit user-selected links to a third-party download API for this default path.
- Finished files are published to Android MediaStore under **Movies / GoreeCloud Clips** on Android 10+. On Android 8–9 they remain in the app-specific Movies folder (not automatically indexed by Gallery).
- Progress is handled asynchronously; only one direct download is active at a time.
- Optional Cobalt-compatible processing endpoint in Settings can be used **only after a direct-download failure and a separate explicit retry**. A direct service URL from the provider is handed to Android DownloadManager.
- External cobalt.tools link remains a separate manual option.

## Download a video

1. Open a TikTok video or share its URL to GoreeCloud Clips.
2. Tap the **floating three-dot button** at the bottom-left of Explore, then the **Download** glyph. If the current page is a recognized video URL, the download confirmation opens immediately; otherwise paste a link.
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
