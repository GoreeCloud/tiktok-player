package com.goreecloud.clips;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONObject;

/** Not exported. Owns mutable cross-process profile registry and explicit shared library. */
public final class ProfileRegistryProvider extends ContentProvider {
    private SharedPreferences metadata, shared;
    @Override public boolean onCreate() {
        metadata = getContext().getSharedPreferences("clips_profiles", 0);
        shared = getContext().getSharedPreferences("clips_explicit_shared", 0);
        return true;
    }
    private String sanitized(String label) {
        if (label == null) return "";
        String value = label.trim().replaceAll("[\\\\r\\\\n\\\\t]", " ");
        return value.length() > 32 ? value.substring(0, 32) : value;
    }
    private boolean exists(int slot) {
        return slot == 0 || (slot > 0 && slot < ProfileRegistry.COUNT &&
                metadata.getBoolean("created_" + slot, false));
    }
    private String name(int slot) {
        return metadata.getString("name_" + slot, slot == 0 ? "Default" : "Profile " + (slot+1));
    }
    @Override public synchronized Bundle call(String method, String arg, Bundle b) {
        if (b == null) b = new Bundle();
        Bundle out = new Bundle();
        int slot = b.getInt("slot", -1);
        switch (method) {
            case "active":
                int active = metadata.getInt("active", 0);
                out.putInt("slot", exists(active) ? active : 0);
                break;
            case "exists": out.putBoolean("yes", exists(slot)); break;
            case "next":
                out.putInt("slot", next()); break;
            case "label":
                out.putString("label", exists(slot) ? name(slot) : "Unknown profile"); break;
            case "activate":
                out.putBoolean("ok", exists(slot) &&
                        metadata.edit().putInt("active", slot).commit());
                break;
            case "create": {
                int fresh = next();
                String label = sanitized(b.getString("label", ""));
                if (fresh > 0) {
                    if (label.isEmpty()) label = "Profile " + (fresh+1);
                    boolean ok = metadata.edit().putBoolean("created_" + fresh, true)
                            .putString("name_" + fresh, label).commit();
                    out.putInt("slot", ok ? fresh : -1);
                } else out.putInt("slot", -1);
                break;
            }
            case "rename": {
                String label = sanitized(b.getString("label", ""));
                out.putBoolean("ok", exists(slot) && !label.isEmpty() &&
                        metadata.edit().putString("name_" + slot, label).commit());
                break;
            }
            case "shared_all": out.putString("items", shared.getString("items", "[]")); break;
            case "shared_save": out.putBoolean("ok", modifyShared(b, true)); break;
            case "shared_remove": out.putBoolean("ok", modifyShared(b, false)); break;
            default: out.putBoolean("ok", false);
        }
        return out;
    }
    private int next() {
        for (int i = 1; i < ProfileRegistry.COUNT; i++)
            if (!metadata.getBoolean("created_" + i, false)) return i;
        return -1;
    }
    private boolean modifyShared(Bundle b, boolean save) {
        String url = b.getString("url", "");
        if (!MediaUrls.isTikTok(url)) return false;
        try {
            JSONArray before = new JSONArray(shared.getString("items", "[]"));
            JSONArray out = new JSONArray();
            if (save) {
                String title = b.getString("title", "Video");
                JSONObject item = new JSONObject();
                item.put("url", url);
                item.put("title", title == null || title.trim().isEmpty() ? "Video" : title.trim());
                out.put(item);
            }
            for (int i = 0; i < before.length() && out.length() < 500; i++) {
                JSONObject item = before.optJSONObject(i);
                if (item != null && MediaUrls.isTikTok(item.optString("url")) &&
                        !url.equals(item.optString("url"))) out.put(item);
            }
            return shared.edit().putString("items", out.toString()).commit();
        } catch (Exception e) { return false; }
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) { return 0; }
}
