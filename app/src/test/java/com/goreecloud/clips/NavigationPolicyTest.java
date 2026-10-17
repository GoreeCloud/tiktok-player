package com.goreecloud.clips;

import org.junit.Test;
import static org.junit.Assert.*;

public final class NavigationPolicyTest {
    @Test public void keepsTikTokVideoSwipesInsidePlayer() {
        assertEquals(NavigationPolicy.Action.IN_WEBVIEW, NavigationPolicy.classify(
                "https://www.tiktok.com/@creator/video/123", true));
        assertEquals(NavigationPolicy.Action.IN_WEBVIEW, NavigationPolicy.classify(
                "https://vm.tiktok.com/abc", true));
        assertEquals(NavigationPolicy.Action.IN_WEBVIEW, NavigationPolicy.classify(
                "https://example.com/embedded", false));
    }

    @Test public void blocksAppStoreAndIntentDeepLinks() {
        String[] links = {
                "https://play.google.com/store/apps/details?id=com.zhiliaoapp.musically.go",
                "https://market.android.com/details?id=com.zhiliaoapp.musically.go",
                "https://apps.samsung.com/appquery/appDetail.as?appId=example",
                "market://details?id=com.zhiliaoapp.musically.go",
                "intent://details?id=com.zhiliaoapp.musically.go#Intent;scheme=market;end"
        };
        for (String link : links) {
            assertEquals(link, NavigationPolicy.Action.BLOCK,
                    NavigationPolicy.classify(link, true));
        }
    }

    @Test public void blocksOneLinkAttributionAndAllOffSiteMainFrames() {
        String[] links = {
                // Observed on an actual OnePlus device during TikTok feed swipes.
                "https://snssdk1340.onelink.me/v0cu/example",
                "https://example.org/article",
                "https://play.google.com.evil.example/",
                "https://another-app.app.link/promo",
                "http://example.org/article",
                "https://127.0.0.1/secret",
                "javascript:alert(1)"
        };
        for (String link : links) {
            assertEquals(link, NavigationPolicy.Action.BLOCK,
                    NavigationPolicy.classify(link, true));
        }
    }

    @Test public void nativeOutboundLinkGuardStillRejectsStoreHosts() {
        assertFalse(NavigationPolicy.isAppStoreUrl("https://play.google.com.evil.example/"));
        assertTrue(NavigationPolicy.isAppStoreUrl("https://play.google.com/store/apps/details"));
        assertTrue(NavigationPolicy.isAppStoreUrl("https://market.android.com/details"));
    }
}
