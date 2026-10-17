package com.goreecloud.clips;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MediaUrlsTest {
    @Test public void acceptsValidVideos() {
        assertTrue(MediaUrls.isVideoLink("https://www.tiktok.com/@person/video/123456"));
        assertTrue(MediaUrls.isVideoLink("https://vm.tiktok.com/example/"));
        assertTrue(MediaUrls.isVideoLink("https://vt.tiktok.com/abcdef/"));
    }
    @Test public void rejectsHostSpoofingAndUnsafeSchemes() {
        assertFalse(MediaUrls.isTikTok("https://tiktok.com.attacker.example/@x/video/1"));
        assertFalse(MediaUrls.isTikTok("http://tiktok.com/@x/video/1"));
        assertFalse(MediaUrls.isTikTok("https://not-tiktok.com/@x/video/1"));
        assertFalse(MediaUrls.isVideoLink("https://www.tiktok.com/"));
    }
    @Test public void rejectsUntrustedProviderOrigins() {
        assertTrue(MediaUrls.isProviderUrl("https://clips.goreecloud.example/"));
        assertFalse(MediaUrls.isProviderUrl("http://clips.example.com/"));
        assertFalse(MediaUrls.isProviderUrl("https://127.0.0.1/"));
        assertFalse(MediaUrls.isProviderUrl("https://192.168.1.6/"));
        assertFalse(MediaUrls.isProviderUrl("https://clips.example.com/api/"));
        assertFalse(MediaUrls.isProviderUrl("https://user:pass@clips.example.com/"));
    }
    @Test public void sanitizesFilenames() {
        assertEquals("video.mp4", MediaUrls.safeName("../../video"));
        assertEquals("clip.mp4", MediaUrls.safeName(".mp4"));
        assertEquals("goreecloud-clip.mp4", MediaUrls.safeName(""));
    }
}
