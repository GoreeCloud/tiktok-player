package com.goreecloud.clips;

import java.net.URI;
import java.util.Locale;

/**
 * Main-frame navigation policy for the embedded TikTok player.
 * Embedded web content must not launch an Android app. Explicit
 * native app actions use their own separate outbound-link handler.
 */
final class NavigationPolicy {
    enum Action { IN_WEBVIEW, BLOCK }

    private NavigationPolicy() {}

    static Action classify(String url, boolean mainFrame) {
        // A WebView "gesture" also covers vertical feed swipes, so it is not
        // proof that the user intended to open an external browser/app.
        // Allow TikTok itself; keep ALL off-site main-frame navigation in Clips.
        // Only explicit native GoreeCloud controls may open external links.
        if (!mainFrame || MediaUrls.isTikTok(url)) return Action.IN_WEBVIEW;
        return Action.BLOCK;
    }

    static boolean isAppStoreUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.ROOT);
            return host.equals("play.google.com")
                    || host.equals("market.android.com")
                    || host.equals("galaxystore.samsung.com")
                    || host.equals("apps.samsung.com")
                    || host.equals("apps.apple.com")
                    || host.equals("appgallery.huawei.com")
                    || host.equals("appgallery.cloud.huawei.com");
        } catch (IllegalArgumentException error) {
            return false;
        }
    }
}
