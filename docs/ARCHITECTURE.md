# GoreeCloud Clips — Architecture

## Modules

| Module | Role |
|---|---|
| MainActivity | Native browser, saved library, Settings, per-download confirmation |
| BookmarkStore | Local saved-link list |
| MediaUrls | HTTPS and TikTok URL validation, filename sanitation |
| CobaltClient | Optional user-selected Cobalt processing server |
| Android DownloadManager | System download notifications and storage |

## Download flow

1. User selects a TikTok video URL.
2. App displays an ownership and authorization warning requiring confirmation.
3. A configured HTTPS provider receives the user-selected URL.
4. When a compatible direct media response is returned, Android DownloadManager queues it.
5. The user sees notification/status in the Android download system.

No processing endpoint is enabled by default. Settings lets the user set a server they operate or have permission to use. Bookmarks remain on-device. Provider interactions and transfers are initiated by explicit user actions.

## Limitations

Provider authentication, slideshow picking, local transcoding, downloaded-video index, Gallery synchronization, and richer collections remain future work. WebView playback and login depend on TikTok's website behavior and require device validation. Copyright warnings cannot verify creator permissions.
