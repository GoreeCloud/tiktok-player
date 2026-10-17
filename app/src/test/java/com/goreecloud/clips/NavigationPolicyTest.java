package com.goreecloud.clips;

import org.junit.Test;
import static org.junit.Assert.*;

public final class NavigationPolicyTest {
    @Test public void keepsTikTokVideoSwipesInsidePlayer() {
        assertEquals(NavigationPolicy.Action.IN_WEBVIEW, NavigationPolicy.classify(
                "https://www.tiktok.com/@creator/video/123", true, false, false));
        assertEquals(NavigationPolicy.Action.IN_WEBVIEW, NavigationPolicy.classify(
                "https://vm.tiktok.com/abc", true, true, false));
        assertEquals(NavigationPolicy.Action.IN_WEBVIEW, NavigationPolicy.classify(
                "https://example.com/embedded", false, true, false));
    }

    @Test public void alwaysBlocksAppStoreAndLiteInstallLinks() {
        String[] links = {
                "https://play.google.com/store/apps/details?id=com.zhiliaoapp.musically.go",
                "https://play.google.com/store/apps/details?id=com.zhiliaoapp.musically",
                "https://market.android.com/details?id=com.zhiliaoapp.musically.go",
                "https://apps.samsung.com/appquery/appDetail.as?appId=example",
                "market://details?id=com.zhiliaoapp.musically.go",
                "intent://details?id=com.zhiliaoapp.musically.go#Intent;scheme=market;end",
                "https://play.google.com.evil.example/store/apps/details"
        };
        for (String link : links) {
            assertEquals(link, NavigationPolicy.Action.BLOCK,
                    NavigationPolicy.classify(link, true, true, false));
        }
        assertEquals(NavigationPolicy.Action.BLOCK, NavigationPolicy.classify(
                "https://play.google.com/store/apps/details", true, true, true));
    }

    @Test public void neverAutoLaunchesOffSiteLinksOrRedirects() {
        assertEquals(NavigationPolicy.Action.BLOCK, NavigationPolicy.classify(
                "https://example.org/article", true, false, false));
        assertEquals(NavigationPolicy.Action.BLOCK, NavigationPolicy.classify(
                "https://example.org/article", true, true, true));
        assertEquals(NavigationPolicy.Action.BLOCK, NavigationPolicy.classify(
                "http://example.org/article", true, true, false));
        assertEquals(NavigationPolicy.Action.BLOCK, NavigationPolicy.classify(
                "https://127.0.0.1/secret", true, true, false));
    }

    @Test public void deliberateSafeExternalLinkNeedsConfirmation() {
        assertEquals(NavigationPolicy.Action.CONFIRM_EXTERNAL, NavigationPolicy.classify(
                "https://example.org/article", true, true, false));
        assertFalse(NavigationPolicy.isAppStoreUrl("https://play.google.com.evil.example/"));
        assertTrue(NavigationPolicy.isAppStoreUrl("https://play.google.com/store/apps/details"));
    }
}
