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

    public static final Set<String> BLOCKED_PACKAGES = new HashSet<>(Arrays.asList(
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

    public static boolean isBlocked(String pkg) { return BLOCKED_PACKAGES.contains(pkg); }
}