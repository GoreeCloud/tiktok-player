package com.goreecloud.clips;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Local, capped video history. No analytics or account sync. */
public final class HistoryStore {
    private static final int LIMIT = 150;
    private final SharedPreferences preferences;
    public HistoryStore(Context context) { this(context, 0); }

    public HistoryStore(Context context, int slot) {
        preferences = context.getSharedPreferences(
                ProfileRegistry.prefName("clips_recent", slot), Context.MODE_PRIVATE);
    }

    public List<BookmarkStore.Item> all() {
        List<BookmarkStore.Item> result = new ArrayList<>();
        try {
            JSONArray items = new JSONArray(preferences.getString("items", "[]"));
            for (int i = 0; i < items.length() && result.size() < LIMIT; i++) {
                JSONObject entry = items.optJSONObject(i);
                if (entry == null) continue;
                String url = entry.optString("url", "");
                if (!MediaUrls.isVideoLink(url)) continue;
                result.add(new BookmarkStore.Item(entry.optString("title", "Recent video"), url));
            }
        } catch (Exception ignored) {
            // Invalid local state should not stop the browser.
        }
        return result;
    }

    public void record(String title, String url) {
        if (!MediaUrls.isVideoLink(url)) return;
        List<BookmarkStore.Item> items = all();
        items.removeIf(item -> item.url.equals(url));
        String display = title == null || title.trim().isEmpty() ? "Recent video" : title.trim();
        items.add(0, new BookmarkStore.Item(display, url));
        if (items.size() > LIMIT) items = new ArrayList<>(items.subList(0, LIMIT));
        write(items);
    }

    public void remove(String url) {
        List<BookmarkStore.Item> items = all();
        items.removeIf(item -> item.url.equals(url));
        write(items);
    }

    public void clear() {
        preferences.edit().remove("items").apply();
    }

    private void write(List<BookmarkStore.Item> items) {
        JSONArray array = new JSONArray();
        for (BookmarkStore.Item item : items) {
            try {
                JSONObject entry = new JSONObject();
                entry.put("title", item.title);
                entry.put("url", item.url);
                array.put(entry);
            } catch (Exception ignored) { }
        }
        preferences.edit().putString("items", array.toString()).apply();
    }
}
