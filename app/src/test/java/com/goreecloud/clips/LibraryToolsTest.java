package com.goreecloud.clips;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import static org.junit.Assert.*;

public class LibraryToolsTest {
    private final BookmarkStore.Item first =
        new BookmarkStore.Item("Home Cooking", "https://www.tiktok.com/@chef/video/101");
    private final BookmarkStore.Item second =
        new BookmarkStore.Item("Travel Guide", "https://www.tiktok.com/@guide/video/202");

    @Test public void queryMatchesTitleIgnoringCase() {
        assertEquals(first.url, LibraryTools.filter(Arrays.asList(first, second), "  COOK ").get(0).url);
        assertEquals(1, LibraryTools.filter(Arrays.asList(first, second), " cook ").size());
    }
    @Test public void queryMatchesUrlAndCreator() {
        assertEquals(second.url, LibraryTools.filter(Arrays.asList(first, second), "@GUIDE").get(0).url);
    }
    @Test public void emptyQueryPreservesOrder() {
        List<BookmarkStore.Item> matches = LibraryTools.filter(Arrays.asList(first, second), "");
        assertEquals(2, matches.size());
        assertSame(first, matches.get(0));
        assertSame(second, matches.get(1));
    }
    @Test public void noMatchesAndEmptyInput() {
        assertTrue(LibraryTools.filter(Arrays.asList(first, second), "nonexistent").isEmpty());
        assertTrue(LibraryTools.filter(Collections.emptyList(), "anything").isEmpty());
    }
}
