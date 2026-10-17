# GoreeCloud Clips

Native Android browser and personal media organizer for TikTok links. This repository is named **GoreeCloud/tiktok-player**, and the application is named **GoreeCloud Clips**.

**Status: development preview, not yet a stable release.** Not affiliated with or endorsed by TikTok.

## Features in the first implementation

- Embedded TikTok browser with external links opened in the system browser.
- Native light-first UI, optional dark mode, and browser navigation.
- Bookmark TikTok pages in a local, searchable-by-reading saved list; open, remove and download from saved entries.
- Android share-sheet support for incoming TikTok links and outgoing shares.
- Rights confirmation before every download action, including website-originated downloads.
- User-configurable, HTTPS-only Cobalt-compatible API instance using the `POST /` JSON contract.
- Android DownloadManager downloads to the public Downloads folder, with notifications.
- Optional manual external downloader link. No tracking, sync, server URL, or authentication token is bundled.

## Download-provider configuration

Open **Settings** and enter the public HTTPS origin of a Cobalt-compatible server that you **operate or have permission to use** (e.g., `https://your-instance.example/`). Public hosted endpoints may disallow third-party clients or require authentication. This version does not support authenticated instances, slideshow pickers or local remuxing. These produce explanatory errors rather than a silent failure.

When you tap **Download**, enter a valid TikTok video link, read and acknowledge the copyright warning, and then the app sends **only that link** to the configured server. If the server returns a supported direct HTTPS media URL, the app queues the transfer using Android DownloadManager. Content rights, regional laws, creator settings, and service terms apply. A warning is not a license. The app does not deliberately remove watermarks or bypass access controls.

A separate **Open cobalt.tools** button opens the official site in your browser, where you can enter links manually. It is not a default public API endpoint.

**Important:** TikTok itself can limit embedded-WebView login, autoplay and downloads. This wrapper does not bypass those restrictions. Video downloads from unofficial third-party tools may not reflect creator permissions, so download only your own content or content for which you have explicit authorization.

## Build

Requires JDK 17, Android SDK Platform 35 and Gradle (wrapper included).

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Development APK: `app/build/outputs/apk/debug/app-debug.apk`. This path only exists after a successful build. Do not distribute production APKs from unsigned or unreviewed source changes.

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/QA.md](docs/QA.md) and [SECURITY.md](SECURITY.md).

## License

Project-original code is released under the MIT license. No TikTok or Cobalt source is vendored into this repository.
