# Security and download boundaries

The application has a built-in, local video extraction engine for user-authorized media. No remote download processing provider is required for this path.

- A separate confirmation warns about copyright and creator permissions before each download. Consent is not a grant of rights.
- The built-in library can contact the site and CDN for the requested video. Requests may fail; no attempt is made to evade access controls or DRM.
- Downloaded videos are first written to an app-specific temporary directory. On Android 10+ complete video files are published to MediaStore; incomplete files are cleaned.
- The app does not transmit WebView session cookies or credentials to a download provider.
- Cobalt-compatible processing is an optional, user-configured fallback and is invoked only after separate user approval on direct-download failure.
- Do not enter secrets or real account credentials into issue reports.
- The app does not include creator-watermark removal functionality.
- Android web session security remains subject to the embedded website's behavior.
- Treat the bundled runtime as third-party executable code. Audit its dependencies, provenance, notices and supported architectures before production release.
