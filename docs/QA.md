# Android validation checklist

## Automated

- `./gradlew :app:testDebugUnitTest` — URL host validation, provider validation, filename sanitization.
- `./gradlew :app:lintDebug` — Android lint.
- `./gradlew :app:assembleDebug` — debug APK assembly.
- GitHub Actions runs all three; see workflow logs for actual results.

## Manual on-device — required before any stable release

1. Confirm empty first-run library, light theme, contrast and system bars.
2. Open TikTok in WebView; test browser Back, home, orientation and exit.
3. Verify login (if the website supports it), video playback and audio.
4. Share a TikTok link to the app; open it and save to the library.
5. Remove a saved link; confirm it disappears after app restart.
6. Toggle dark mode and check system status/navigation bar legibility.
7. Enter malformed / http / loopback provider URL and ensure rejection.
8. Test without configured provider and verify Settings prompt.
9. Test authorized known-good downloadable content against an operator-approved Cobalt API endpoint; confirm rights warning, notification and playable output.
10. Test 401, 403, 429, timeout, unsupported picker/remux response and network loss; verify no false success.
11. Verify site-originated direct downloads always show the copyright warning.
12. Verify that the app does not send browser session cookies to a provider and does not strip creator attribution.

## Known limitations

- Actual downloads require a compatible authorized provider that returns HTTPS tunnel/redirect URLs.
- No Cobalt API key, picker/slideshow, remux pipeline or background retries.
- The wrapper does not know whether a creator enabled downloads; the user must respect creator rights.
- TikTok WebView authentication/playback may break when the website changes.
- UI has not been validated for tablets, accessibility services or localization yet.
