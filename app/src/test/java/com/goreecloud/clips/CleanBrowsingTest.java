package com.goreecloud.clips;

import org.junit.Test;
import static org.junit.Assert.*;

public class CleanBrowsingTest {
    @Test public void adHostsAreBlocked() {
        assertTrue(CleanBrowsing.shouldBlockRequest("https://g.doubleclick.net/ads", false));
        assertTrue(CleanBrowsing.shouldBlockRequest("https://ads.pubmatic.com/x", false));
    }
    @Test public void tiktokVideoLoadsNormally() {
        assertFalse(CleanBrowsing.shouldBlockRequest("https://www.tiktok.com/@a/video/123", false));
        assertFalse(CleanBrowsing.shouldBlockRequest("https://v16.tiktokcdn.com/v.mp4", false));
    }
    @Test public void unrelatedLinksAndTopFramesPass() {
        assertFalse(CleanBrowsing.shouldBlockRequest("https://doubleclick.net/", true));
        assertFalse(CleanBrowsing.shouldBlockRequest("https://doubleclick.net.example.org/", false));
        assertFalse(CleanBrowsing.shouldBlockRequest("https://example.com/?q=doubleclick.net", false));
        assertFalse(CleanBrowsing.shouldBlockRequest(null, false));
    }
}
