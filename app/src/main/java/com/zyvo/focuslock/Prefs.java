package com.zyvo.focuslock;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Prefs {
    private static final String NAME = "focuslock_prefs";
    private static final String KEY_END_TIME = "timer_end_time";
    private static final String KEY_ACTIVE = "timer_active";
    private static final String KEY_DURATION = "timer_duration_min";
    private static final String KEY_BLOCKED = "blocked_packages";

    public static final Set<String> DEFAULT_BLOCKED_PACKAGES = new HashSet<>(Arrays.asList(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.instagram.android",
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.zhiliaoapp.musically",   // TikTok
        "com.twitter.android",
        "com.snapchat.android",
        "com.reddit.frontpage",
        "com.netflix.mediaclient",
        "com.spotify.music"
    ));

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static long getEndTime(Context c) { return sp(c).getLong(KEY_END_TIME, 0L); }
    public static void setEndTime(Context c, long v) { sp(c).edit().putLong(KEY_END_TIME, v).apply(); }

    public static boolean isActive(Context c) { return sp(c).getBoolean(KEY_ACTIVE, false); }
    public static void setActive(Context c, boolean v) { sp(c).edit().putBoolean(KEY_ACTIVE, v).apply(); }

    public static int getDurationMin(Context c) { return sp(c).getInt(KEY_DURATION, 25); }
    public static void setDurationMin(Context c, int v) { sp(c).edit().putInt(KEY_DURATION, v).apply(); }

    public static Set<String> getBlocked(Context c) {
        Set<String> s = sp(c).getStringSet(KEY_BLOCKED, null);
        return s != null ? s : DEFAULT_BLOCKED_PACKAGES;
    }

    public static void toggleBlocked(Context c, String pkg) {
        Set<String> s = new HashSet<>(getBlocked(c));
        if (!s.remove(pkg)) s.add(pkg);
        sp(c).edit().putStringSet(KEY_BLOCKED, s).apply();
    }

    public static boolean isBlocked(Context c, String pkg) { return getBlocked(c).contains(pkg); }

    public static void setWatchdogTick(Context c) { sp(c).edit().putLong("wd_tick", System.currentTimeMillis()).apply(); }
    public static long getWatchdogTick(Context c) { return sp(c).getLong("wd_tick", 0L); }
}