package com.goreecloud.clips;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pure, testable bookmark/history filtering; preserves source order. */
public final class LibraryTools {
    private LibraryTools() {}
    public static List<BookmarkStore.Item> filter(List<BookmarkStore.Item> input, String query) {
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (term.isEmpty()) return new ArrayList<>(input);
        List<BookmarkStore.Item> out = new ArrayList<>();
        for (BookmarkStore.Item entry : input) {
            if (entry.title.toLowerCase(Locale.ROOT).contains(term)
                    || entry.url.toLowerCase(Locale.ROOT).contains(term)) {
                out.add(entry);
            }
        }
        return out;
    }
}
