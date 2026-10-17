# GoreeCloud Clips — Architecture

## Core modules

| Module | Responsibility |
|---|---|
| MainActivity | WebView, native controls, bookmarks, Settings, explicit rights confirmation, download status |
| BookmarkStore | Local saved TikTok links |
| MediaUrls | URL and basic hostname validation |
| DirectDownloader | On-device yt-dlp execution, per-download temporary files, MediaStore publishing, cleanup |
| CobaltClient | Optional, user-selected Cobalt processing server for explicit fallback |
| Android DownloadManager | Transfer returned HTTPS URL for the optional server fallback |

## Primary native download flow

1. User chooses **Download**, and provides a valid TikTok link when necessary.
2. User acknowledges the ownership and permissions notice.
3. DirectDownloader calls the bundled yt-dlp Android runtime on a background executor.
4. A unique private temporary directory contains the work in progress.
5. On success, a completed media file is imported into Movies/GoreeCloud Clips on Android 10+, and temporary files are removed.
6. On failure, the app shows the actual failure. If an optional trusted server was configured, the app offers a **separate user-initiated retry** through it; no silent fallback occurs.

The native engine is the default and needs no remote download service. The bundled free runtime may fail against TikTok's requests due to website/TLS changes, private media or restricted access. It is not a DRM bypass. It does not export the WebView's cookies to third parties.

## Data retention

Bookmarks remain local in SharedPreferences. Completed Android 10+ files belong to the user-visible MediaStore Movies collection. On Android 8–9, files remain in app-specific external Movies storage.

## Future work

- Background progress notifications, cancellation, retry history and a downloaded-media index.
- Android 8–9 public saving through Storage Access Framework.
- Provider selection, approved credentials and explicit permission checks.
- Galleries, tags, playlists and downloads management.
- Device QA of TikTok WebView playback and native extractor behavior.
