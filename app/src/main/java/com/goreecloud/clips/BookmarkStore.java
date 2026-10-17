package com.goreecloud.clips;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class BookmarkStore {
    public static final class Item {
        public final String title;
        public final String url;
        public Item(String title, String url) { this.title = title; this.url = url; }
    }

    private final SharedPreferences preferences;
    public BookmarkStore(Context context) {
        preferences = context.getSharedPreferences("clips_library", Context.MODE_PRIVATE);
    }

    public List<Item> all() {
        List<Item> items = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(preferences.getString("items", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject entry = array.optJSONObject(i);
                if (entry == null) continue;
                String url = entry.optString("url");
                if (MediaUrls.isTikTok(url))
                    items.add(new Item(entry.optString("title", "Saved video"), url));
            }
        } catch (Exception ignored) { /* Never crash due to corrupt local preferences. */ }
        return items;
    }

    public void save(String title, String url) {
        if (!MediaUrls.isTikTok(url)) return;
        List<Item> items = all();
        items.removeIf(item -> item.url.equals(url));
        items.add(0, new Item(title == null || title.trim().isEmpty() ? "Saved video" : title, url));
        if (items.size() > 500) items = new ArrayList<>(items.subList(0, 500));
        write(items);
    }

    public void remove(String url) {
        List<Item> items = all();
        items.removeIf(item -> item.url.equals(url));
        write(items);
    }

    public boolean contains(String url) {
        for (Item item : all()) if (item.url.equals(url)) return true;
        return false;
    }

    private void write(List<Item> items) {
        JSONArray out = new JSONArray();
        for (Item item : items) {
            JSONObject entry = new JSONObject();
            try {
                entry.put("title", item.title);
                entry.put("url", item.url);
                out.put(entry);
            } catch (Exception ignored) { }
        }
        preferences.edit().putString("items", out.toString()).apply();
    }
}
