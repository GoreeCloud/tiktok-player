package com.goreecloud.clips;

import java.net.URI;
import java.util.Locale;

/** Conservative request host filtering; never blocks first-party TikTok media/CDN. */
public final class CleanBrowsing {
    private CleanBrowsing() {}

    private static final String[] AD_HOSTS = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "adnxs.com", "criteo.com", "criteo.net",
        "taboola.com", "outbrain.com", "pubmatic.com", "rubiconproject.com",
        "openx.net", "adsrvr.org", "scorecardresearch.com",
        "quantserve.com", "casalemedia.com", "moatads.com",
        "amazon-adsystem.com", "advertising.com", "smartadserver.com",
        "demdex.net", "yieldmo.com"
    };

    public static boolean shouldBlockRequest(String url, boolean isMainFrame) {
        if (isMainFrame || url == null) return false;
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    && !"http".equalsIgnoreCase(uri.getScheme())) return false;
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.ROOT);
            for (String blocked : AD_HOSTS) {
                if (host.equals(blocked) || host.endsWith("." + blocked)) return true;
            }
        } catch (IllegalArgumentException ignored) {
            return false;
        }
        return false;
    }
}
