package com.zyvo.focuslock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import androidx.core.app.NotificationCompat;

public class TimerService extends Service {
    public static final String CHANNEL_ID = "focuslock_timer";
    public static final int NOTIF_ID = 1001;
    public static final String ACTION_START = "com.zyvo.focuslock.START";
    public static final String ACTION_STOP = "com.zyvo.focuslock.STOP";
    public static final String EXTRA_MINUTES = "minutes";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tick;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            // Timer cannot be stopped by user; ignore stop requests while active.
            if (Prefs.isActive(this)) {
                scheduleTick();
                return START_STICKY;
            }
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_START.equals(intent.getAction()) && Prefs.isActive(this)) {
            // Timer already running; only re-attach the foreground notification. Never reset the deadline.
            startForeground(NOTIF_ID, buildNotification(remainingMs()));
            scheduleTick();
            return START_STICKY;
        }
        int minutes = 25;
        if (intent != null) minutes = intent.getIntExtra(EXTRA_MINUTES, 25);
        if (minutes < 1) minutes = 1;
        if (minutes > 720) minutes = 720;
        long end = System.currentTimeMillis() + (long) minutes * 60 * 1000L;
        Prefs.setDurationMin(this, minutes);
        Prefs.setEndTime(this, end);
        Prefs.setActive(this, true);

        startForeground(NOTIF_ID, buildNotification(remainingMs()));
        scheduleTick();
        return START_STICKY;
    }

    private void scheduleTick() {
        if (tick != null) handler.removeCallbacks(tick);
        tick = new Runnable() {
            @Override public void run() {
                long rem = remainingMs();
                if (rem <= 0) {
                    finishTimer();
                    return;
                }
                enforceBlock();
                updateNotification(rem);
                handler.postDelayed(this, 500);
            }
        };
        handler.postDelayed(tick, 1000);
    }

    private long remainingMs() {
        return Prefs.getEndTime(this) - System.currentTimeMillis();
    }

    private void updateNotification(long rem) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, buildNotification(rem));
    }

    /**
     * Poll UsageStatsManager for the current foreground app and redirect if blocked.
     * Needs PACKAGE_USAGE_STATS ("Usage access") granted by the user.
     */
    private void enforceBlock() {
        Prefs.setWatchdogTick(this);
        String pkg = foregroundPkg();
        if (pkg == null || pkg.equals(getPackageName())) return;
        if (!Prefs.isBlocked(this, pkg)) return;
        Intent i = new Intent(this, BlockedAppsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
            | Intent.FLAG_ACTIVITY_NO_HISTORY);
        try {
            startActivity(i);
        } catch (Exception ex) {
            // bg-activity-start denied (overlay permission missing); retry next tick
        }
    }

    private String foregroundPkg() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        if (usm == null) return null;
        long now = System.currentTimeMillis();
        // Latest RESUMED event in a wide window; some OEMs report usage events sparsely.
        UsageEvents ev = usm.queryEvents(now - 15000, now);
        UsageEvents.Event e = new UsageEvents.Event();
        String last = null;
        long lastTs = -1;
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e);
            if (e.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED && e.getTimeStamp() > lastTs) {
                lastTs = e.getTimeStamp();
                last = e.getPackageName();
            }
        }
        if (last != null) return last;
        // Fallback: most recently used app in the last 10 seconds.
        java.util.List<UsageStats> stats =
            usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 10000, now);
        if (stats == null || stats.isEmpty()) return null;
        UsageStats best = stats.get(0);
        for (UsageStats s : stats) if (s.getLastTimeUsed() > best.getLastTimeUsed()) best = s;
        return best.getPackageName();
    }

    private void finishTimer() {
        Prefs.setActive(this, false);
        Prefs.setEndTime(this, 0L);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("FocusLock")
            .setContentText("Timer complete! Focus session done.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build());
        stopForeground(STOP_FOREGROUND_DETACH);
        stopSelf();
    }

    private Notification buildNotification(long rem) {
        long sec = Math.max(0, rem / 1000);
        long m = sec / 60;
        long s = sec % 60;
        String text = String.format("Focus active - %02d:%02d remaining (cannot be stopped)", m, s);
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("FocusLock - Timer running")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Focus Timer",
                NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Persistent focus timer notification");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        if (tick != null) handler.removeCallbacks(tick);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    public static void start(Context c, int minutes) {
        Intent i = new Intent(c, TimerService.class);
        i.setAction(ACTION_START);
        i.putExtra(EXTRA_MINUTES, minutes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            c.startForegroundService(i);
        } else {
            c.startService(i);
        }
    }

    public static void resumeIfNeeded(Context c) {
        if (Prefs.isActive(c) && Prefs.getEndTime(c) > System.currentTimeMillis()) {
            // Re-attach foreground service to keep running
            Intent i = new Intent(c, TimerService.class);
            i.setAction(ACTION_START);
            i.putExtra(EXTRA_MINUTES, (int) ((Prefs.getEndTime(c) - System.currentTimeMillis()) / 60000L) + 1);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                c.startForegroundService(i);
            } else {
                c.startService(i);
            }
        } else if (Prefs.isActive(c)) {
            // Timer expired while service was dead
            Prefs.setActive(c, false);
            Prefs.setEndTime(c, 0L);
        }
    }
}