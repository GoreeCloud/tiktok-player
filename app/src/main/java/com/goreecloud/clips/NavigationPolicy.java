package com.goreecloud.clips;

import java.net.URI;
import java.util.Locale;

/**
 * Main-frame navigation policy for the embedded TikTok player.
 * Untrusted web content must never launch an Android app without an
 * explicit, separate confirmation from the user.
 */
final class NavigationPolicy {
    enum Action { IN_WEBVIEW, BLOCK, CONFIRM_EXTERNAL }

    private NavigationPolicy() {}

    static Action classify(String url, boolean mainFrame, boolean hasGesture,
            boolean redirect) {
        if (!mainFrame) return Action.IN_WEBVIEW;
        if (MediaUrls.isTikTok(url)) return Action.IN_WEBVIEW;
        if (!hasGesture || redirect || !MediaUrls.isDownloadUrl(url)
                || isAppStoreUrl(url)) return Action.BLOCK;
        return Action.CONFIRM_EXTERNAL;
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
