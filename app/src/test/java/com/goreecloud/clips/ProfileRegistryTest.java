package com.goreecloud.clips;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ProfileRegistryTest {
    @Test public void defaultKeepsLegacyPreferenceKeys() {
        assertEquals("clips_library", ProfileRegistry.prefName("clips_library", 0));
        assertEquals("clips_recent", ProfileRegistry.prefName("clips_recent", 0));
        assertEquals("goreecloud_clips_prefs",
                ProfileRegistry.prefName("goreecloud_clips_prefs", 0));
    }
    @Test public void additionalSlotsNeverMixPreferenceFiles() {
        assertEquals("clips_library_profile_1",
                ProfileRegistry.prefName("clips_library", 1));
        assertEquals("clips_library_profile_2",
                ProfileRegistry.prefName("clips_library", 2));
        assertNotEquals(ProfileRegistry.prefName("clips_recent", 1),
                ProfileRegistry.prefName("clips_recent", 3));
    }
    @Test(expected = IllegalArgumentException.class)
    public void invalidProfileIsRejected() { ProfileRegistry.prefName("x", 4); }
}
