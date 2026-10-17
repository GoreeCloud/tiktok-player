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
        public final boolean favorite;
        public Item(String title, String url) { this(title, url, false); }
        public Item(String title, String url, boolean favorite) {
            this.title = title; this.url = url; this.favorite = favorite;
        }
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
                    items.add(new Item(entry.optString("title", "Saved video"), url,
                            entry.optBoolean("favorite", false)));
            }
        } catch (Exception ignored) { /* Never crash due to corrupt local preferences. */ }
        items.sort((a, b) -> Boolean.compare(b.favorite, a.favorite));
        return items;
    }

    public void save(String title, String url) {
        if (!MediaUrls.isTikTok(url)) return;
        List<Item> items = all();
        boolean favorite = containsFavorite(items, url);
        items.removeIf(item -> item.url.equals(url));
        items.add(0, new Item(title == null || title.trim().isEmpty() ? "Saved video" : title, url, favorite));
        if (items.size() > 500) items = new ArrayList<>(items.subList(0, 500));
        write(items);
    }

    public void remove(String url) {
        List<Item> items = all();
        items.removeIf(item -> item.url.equals(url));
        write(items);
    }

    public void toggleFavorite(String url) {
        List<Item> items = all();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (item.url.equals(url)) {
                items.set(i, new Item(item.title, item.url, !item.favorite));
                write(items);
                return;
            }
        }
    }

    private boolean containsFavorite(List<Item> items, String url) {
        for (Item item : items) if (item.url.equals(url)) return item.favorite;
        return false;
    }

    public void clear() { preferences.edit().remove("items").apply(); }

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
                entry.put("favorite", item.favorite);
                out.put(entry);
            } catch (Exception ignored) { }
        }
        preferences.edit().putString("items", out.toString()).apply();
    }
}
