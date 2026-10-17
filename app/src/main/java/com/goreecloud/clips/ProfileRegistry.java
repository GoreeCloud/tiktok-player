package com.goreecloud.clips;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** IPC facade for the single-process profile metadata and shared collection provider. */
public final class ProfileRegistry {
    public static final int COUNT = 4;
    private ProfileRegistry() {}
    public static String prefName(String base, int slot) {
        if (slot < 0 || slot >= COUNT) throw new IllegalArgumentException("Invalid profile");
        return slot == 0 ? base : base + "_profile_" + slot;
    }
    public static Class<?> activityForSlot(int slot) {
        switch (slot) {
            case 0: return MainActivity.class;
            case 1: return ProfileActivity1.class;
            case 2: return ProfileActivity2.class;
            case 3: return ProfileActivity3.class;
            default: throw new IllegalArgumentException("Invalid profile");
        }
    }
    private static Bundle call(Context context, String method, Bundle request) {
        try {
            Uri uri = Uri.parse("content://" + context.getPackageName() + ".profiles");
            Bundle output = context.getContentResolver().call(uri, method, null, request);
            return output == null ? new Bundle() : output;
        } catch (RuntimeException e) { return new Bundle(); }
    }
    private static Bundle slot(int slot) {
        Bundle b = new Bundle(); b.putInt("slot", slot); return b;
    }
    public static int active(Context c) { return call(c, "active", null).getInt("slot", 0); }
    public static boolean exists(Context c, int s) {
        return s == 0 || (s > 0 && s < COUNT &&
                call(c, "exists", slot(s)).getBoolean("yes", false));
    }
    public static int nextAvailable(Context c) {
        return call(c, "next", null).getInt("slot", -1);
    }
    public static String label(Context c, int s) {
        return call(c, "label", slot(s)).getString("label", s == 0 ? "Default" : "Profile " + (s+1));
    }
    public static boolean activate(Context c, int s) {
        return call(c, "activate", slot(s)).getBoolean("ok", false);
    }
    public static int create(Context c, String label) {
        if (android.os.Build.VERSION.SDK_INT < 28) return -1;
        Bundle b = new Bundle(); b.putString("label", label);
        return call(c, "create", b).getInt("slot", -1);
    }
    public static boolean rename(Context c, int s, String label) {
        Bundle b = slot(s); b.putString("label", label);
        return call(c, "rename", b).getBoolean("ok", false);
    }
    public static List<BookmarkStore.Item> sharedAll(Context c) {
        List<BookmarkStore.Item> result = new ArrayList<>();
        String json = call(c, "shared_all", null).getString("items", "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length() && result.size() < 500; i++) {
                JSONObject e = array.optJSONObject(i);
                if (e != null && MediaUrls.isTikTok(e.optString("url")))
                    result.add(new BookmarkStore.Item(e.optString("title", "Video"),
                            e.optString("url")));
            }
        } catch (Exception ignored) { }
        return result;
    }
    public static boolean sharedSave(Context c, String title, String url) {
        Bundle b = new Bundle(); b.putString("title", title); b.putString("url", url);
        return call(c, "shared_save", b).getBoolean("ok", false);
    }
    public static boolean sharedRemove(Context c, String url) {
        Bundle b = new Bundle(); b.putString("url", url);
        return call(c, "shared_remove", b).getBoolean("ok", false);
    }
}
