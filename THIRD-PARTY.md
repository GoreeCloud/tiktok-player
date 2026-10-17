# Third-party components

GoreeCloud Clips integrates the independently maintained MIT-licensed yt-dlp Android runtime:

- Dependency: `dev.ffmpegkit-maintained:yt-dlp-android:2.0.2`
- Source: https://github.com/ffmpegkit-maintained/yt-dlp-android
- License: MIT (subject to bundled third-party notices)
- The AAR packages Python / yt-dlp and other supporting components. Their separate license details are documented in the upstream `THIRD-PARTY-NOTICES.txt` file, which is also included in the AAR assets at runtime.

No source code or licensed trademark from TikTok is included. GoreeCloud Clips is an independent client.

This dependency increases APK size and supports only arm64-v8a and x86_64 according to its upstream documentation. Revalidate bundled notices and licenses when upgrading.
